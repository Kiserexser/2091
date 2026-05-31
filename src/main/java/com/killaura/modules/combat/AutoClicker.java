package com.killaura.modules.combat;

import com.killaura.modules.Module;
import com.killaura.settings.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import java.util.Random;

public class AutoClicker extends Module {
    public final FloatSetting   minCPS     = flt("MinCPS",    "Min clicks/sec",    10.0f,1.0f,20.0f,0.5f);
    public final FloatSetting   maxCPS     = flt("MaxCPS",    "Max clicks/sec",    14.0f,1.0f,20.0f,0.5f);
    public final BooleanSetting rightClick = bool("RightClick","Auto right-click",  false);
    public final BooleanSetting onHold     = bool("OnHold",   "Only while holding LMB",true);
    private long lastClickTime = 0, currentInterval = 100;
    private final Random rng = new Random();

    public AutoClicker() { super("AutoClicker","Clicks at randomized CPS",Category.COMBAT,GLFW.GLFW_KEY_UNKNOWN); }
    @Override public void onEnable()  { currentInterval = nextInterval(); }
    @Override public void onDisable() {}

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (onHold.getValue() && !mc.options.attackKey.isPressed()) return;
        long now = System.currentTimeMillis();
        if (now - lastClickTime < currentInterval) return;
        if (mc.targetedEntity != null) { mc.options.attackKey.setPressed(true); mc.player.swingHand(Hand.MAIN_HAND); }
        if (rightClick.getValue() && mc.targetedEntity == null) mc.options.useKey.setPressed(true);
        lastClickTime = now;
        currentInterval = nextInterval();
    }

    private long nextInterval() {
        float min = Math.min(minCPS.getValue(), maxCPS.getValue());
        float max = Math.max(minCPS.getValue(), maxCPS.getValue());
        float cps = min + rng.nextFloat() * (max - min);
        return Math.max(45L, (long)(1000f / cps));
    }
}
