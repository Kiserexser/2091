package com.killaura.modules.movement;

import com.killaura.modules.Module;
import com.killaura.settings.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import org.lwjgl.glfw.GLFW;

public class Sprint extends Module {
    public final BooleanSetting omni   = bool("OmniSprint", "Sprint in all directions",false);
    public final BooleanSetting legit  = bool("LegitSprint","Mimic natural sprint",    true);

    public Sprint() { super("Sprint","Auto sprints",Category.MOVEMENT,GLFW.GLFW_KEY_V); }
    @Override public void onEnable()  {}
    @Override public void onDisable() { MinecraftClient mc = MinecraftClient.getInstance(); if (mc.player != null) mc.player.setSprinting(false); }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return;
        if (mc.player.getHungerManager().getFoodLevel() <= 6) return;
        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || mc.player.isBlocking()) return;
        boolean moving = mc.player.input.movementForward > 0;
        if (omni.getValue()) moving |= mc.player.input.movementSideways != 0;
        if (legit.getValue()) { if (moving && mc.player.input.movementForward > 0) mc.player.setSprinting(true); }
        else { if (moving) mc.player.setSprinting(true); }
    }
}
