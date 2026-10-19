package local_port_server_checks;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Offscreen framebuffer capture for the disposable client; never shipped. */
@EventBusSubscriber(modid = "local_port_server_checks", value = Dist.CLIENT)
public final class ClientChecks {
    private static int ticks;
    private static int totalTicks;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        totalTicks++;
        if (totalTicks == 200) {
            Screenshot.grab(client.gameDirectory, "port-startup.png",
                    client.gameRenderer.mainRenderTarget(), 1,
                    message -> LogUtils.getLogger().info("LOCAL_PORT_FRAME {}", message.getString()));
        }
        if (client.level == null || client.player == null) return;
        ticks++;
        if (ticks % 200 == 0) {
            Screenshot.grab(client.gameDirectory, "port-frame-" + ticks + ".png",
                    client.gameRenderer.mainRenderTarget(), 1,
                    message -> LogUtils.getLogger().info("LOCAL_PORT_FRAME {}", message.getString()));
        }
        if (ticks == 2400) client.stop();
    }
}
