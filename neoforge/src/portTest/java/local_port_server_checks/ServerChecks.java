package local_port_server_checks;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import immersive_aircraft.cobalt.registration.CobaltFuelRegistry;
import immersive_aircraft.neoforge.cobalt.registration.CobaltFuelRegistryImpl;
import immersive_aircraft.screen.VehicleScreenHandler;
import immersive_machinery.Entities;
import immersive_machinery.entity.BambooBee;
import immersive_machinery.network.c2s.BambooBeeConfigurationUpdate;
import immersive_machinery.network.c2s.TunnelDiggerControlsUpdate;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mod("local_port_server_checks")
public final class ServerChecks {
    private static final Logger LOGGER = LogUtils.getLogger();

    public ServerChecks() {
        NeoForge.EVENT_BUS.addListener(this::started);
    }

    private void started(ServerStartedEvent event) {
        event.getServer().execute(() -> run(event.getServer()));
    }

    private void run(MinecraftServer server) {
        Map<String, String> results = new LinkedHashMap<>();
        check(results, "vanilla_fuel", () -> {
            ServerLevel level = server.overworld();
            require(level != null, "A real server level is required; JUnit's data-only server has none");
            var furnace = new FurnaceProbe();
            furnace.setLevel(level);
            require(furnace.getBurnDuration(level, new ItemStack(Items.COAL)) == 1600,
                    "Vanilla control experiment must burn coal for 1600 ticks");
            CobaltFuelRegistryImpl.setFuelValues(level);
            require(CobaltFuelRegistry.INSTANCE.get(new ItemStack(Items.COAL)) == 1600,
                    "Aircraft must preserve the vanilla coal burn time");
            require(CobaltFuelRegistry.INSTANCE.get(new ItemStack(Items.STONE)) == 0,
                    "Stone must not become fuel");
        });
        check(results, "drill_authority_and_bounds", () -> drillControls(server.overworld()));
        check(results, "bee_menu_authority_and_range", () -> beeConfiguration(server.overworld()));
        try {
            Files.writeString(Path.of("local-port-server-checks.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(results));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        LOGGER.info("LOCAL_PORT_SERVER_CHECKS {}", results);
    }

    private static void check(Map<String, String> results, String name, Runnable test) {
        try {
            test.run();
            results.put(name, "PASS");
        } catch (RuntimeException | AssertionError failure) {
            results.put(name, "FAIL: " + failure);
            LOGGER.error("Local port regression failed: {}", name, failure);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void drillControls(ServerLevel level) {
        var digger = Entities.TUNNEL_DIGGER.get().create(level, EntitySpawnReason.COMMAND);
        require(digger != null, "Tunnel digger must instantiate");
        var player = new ConnectedTestPlayer(level);
        player.setPos(0, 64, 8);
        digger.setPos(0, 64, 8);
        try {
            new TunnelDiggerControlsUpdate(1, true).receive(player);
            require(!digger.drilling && digger.drillY == 0, "Non-rider must not control the digger");
            require(player.startRiding(digger), "Connected test pilot must be able to mount");
            require(digger.getControllingPassenger() == player, "Fixture must be the actual controlling passenger");
            new TunnelDiggerControlsUpdate(Integer.MAX_VALUE, true).receive(player);
            require(!digger.drilling && digger.drillY == 0, "Huge positive drill offset must be rejected");
            new TunnelDiggerControlsUpdate(Integer.MIN_VALUE, true).receive(player);
            require(!digger.drilling && digger.drillY == 0, "Huge negative drill offset must be rejected");
            new TunnelDiggerControlsUpdate(-1, true).receive(player);
            require(digger.drilling && digger.drillY == -1, "Valid pilot controls must remain functional");
            new TunnelDiggerControlsUpdate(0, false).receive(player);
            require(!digger.drilling && digger.drillY == 0, "Valid stop command must remain functional");
        } finally {
            player.stopRiding();
            digger.discard();
        }
    }

    private static void beeConfiguration(ServerLevel level) {
        var bee = Entities.BAMBOO_BEE.get().create(level, EntitySpawnReason.COMMAND);
        require(bee != null, "Bamboo bee must instantiate");
        var player = new ConnectedTestPlayer(level);
        bee.setPos(0, 64, 12);
        player.setPos(0, 64, 12);
        require(level.addFreshEntity(bee), "Bee must be present in the disposable world for ID lookup");
        var oldMenu = player.containerMenu;
        try {
            configuration(bee, true).receive(player);
            require(!bee.getConfiguration().blacklist, "Configuration without the entity's menu must be rejected");
            player.containerMenu = new VehicleScreenHandler(17, player.getInventory(), bee);
            configuration(bee, true).receive(player);
            require(bee.getConfiguration().blacklist, "Valid local menu edit must apply");
            player.setPos(1000, 64, 12);
            configuration(bee, false).receive(player);
            require(bee.getConfiguration().blacklist, "Remote stale-menu edit must be rejected");
        } finally {
            if (player.containerMenu != oldMenu) player.containerMenu.removed(player);
            player.containerMenu = oldMenu;
            bee.discard();
        }
    }

    private static BambooBeeConfigurationUpdate configuration(BambooBee bee, boolean blacklist) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), bee.level().registryAccess());
        try {
            buffer.writeInt(bee.getId());
            var desired = new BambooBee.Configuration();
            desired.blacklist = blacklist;
            desired.encode(buffer);
            return new BambooBeeConfigurationUpdate(buffer);
        } finally {
            buffer.release();
        }
    }

    // Simulate a live network participant while using NeoForge's own fake-player
    // implementation; no real account, network connection or production player.
    private static class ConnectedTestPlayer extends ServerPlayer {
        private ConnectedTestPlayer(ServerLevel level) {
            super(level.getServer(), level,
                    new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000002603"), "PortTest"),
                    ClientInformation.createDefault());
            // Public no-network packet sink. The actor remains a normal
            // ServerPlayer: NeoForge's FakePlayer deliberately refuses riding.
            connection = FakePlayerFactory.getMinecraft(level).connection;
        }

        @Override
        public boolean hasDisconnected() {
            return false;
        }
    }

    private static class FurnaceProbe extends FurnaceBlockEntity {
        private FurnaceProbe() {
            super(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        @Override
        public int getBurnDuration(ServerLevel level, ItemStack stack) {
            return super.getBurnDuration(level, stack);
        }
    }
}
