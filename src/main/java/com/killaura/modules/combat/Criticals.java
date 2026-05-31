package com.killaura.modules.combat;

import com.killaura.modules.Module;
import com.killaura.settings.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.lwjgl.glfw.GLFW;
import java.util.Random;

public class Criticals extends Module {
    public final ModeSetting    mode        = mode("Mode",     "Crit method",   "Packet","Packet","Jump","None");
    public final BooleanSetting onlyOnGround= bool("OnGround", "Only when on ground", true);
    public final FloatSetting   chance      = flt("Chance",    "Chance to crit",1.0f, 0.0f, 1.0f, 0.05f);
    private boolean pending = false;
    private final Random rng = new Random();

    public Criticals() { super("Criticals", "Forces critical hits", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN); }
    @Override public void onEnable()  {}
    @Override public void onDisable() { pending = false; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !pending) return;
        if (onlyOnGround.getValue() && !mc.player.isOnGround()) { pending = false; return; }
        if (rng.nextFloat() > chance.getValue()) { pending = false; return; }
        switch (mode.getValue()) {
            case "Packet" -> sendPacketCrit(mc);
            case "Jump"   -> mc.player.jump();
        }
        pending = false;
    }

    private void sendPacketCrit(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null) return;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.1E-5, z, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true));
    }

    public void scheduleCrit() { pending = true; }
}
