package immersive_machinery.network.c2s;

import immersive_aircraft.cobalt.network.Message;
import immersive_machinery.entity.TunnelDigger;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

public class TunnelDiggerControlsUpdate extends Message {
    private final int drillY;
    private final boolean drilling;

    public TunnelDiggerControlsUpdate(int drillY, boolean drilling) {
        this.drillY = drillY;
        this.drilling = drilling;
    }

    public TunnelDiggerControlsUpdate(RegistryFriendlyByteBuf b) {
        drillY = b.readInt();
        drilling = b.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeInt(drillY);
        b.writeBoolean(drilling);
    }

    @Override
    public void receive(Player e) {
        if (e instanceof ServerPlayer player && !player.hasDisconnected() && player.isAlive()
                && !player.isRemoved() && drillY >= -1 && drillY <= 1
                && player.getVehicle() instanceof TunnelDigger entity
                && entity.isAlive() && entity.getControllingPassenger() == player) {
            entity.drillY = drillY;
            entity.drilling = drilling;
        }
    }
}
