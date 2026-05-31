package com.killaura.modules.combat;

import com.killaura.modules.Module;
import com.killaura.settings.*;
import org.lwjgl.glfw.GLFW;
import java.util.Random;

public class Reach extends Module {
    public final FloatSetting   dist      = flt("Range",   "Base reach",           3.05f,3.00f,6.00f,0.01f);
    public final BooleanSetting randomize = bool("Randomize","Per-hit randomization",true);
    public final FloatSetting   randAmt   = flt("RandAmt", "Randomization ±",      0.005f,0.0f,0.05f,0.001f);
    public final ModeSetting    bypass    = mode("Bypass",  "AC cap profile",       "Grim",
                                                 "Grim","Vulcan","Polar","Spartan","Matrix","AAC","Verus","None");

    private static final float CAP_GRIM_MIN=3.040f, CAP_GRIM_MAX=3.045f, CAP_VULCAN=3.15f, CAP_POLAR=3.40f, CAP_UNIVERSAL=3.44f;
    private final Random rng = new Random();

    public Reach() { super("Reach","Extends attack range with AC-safe caps",Category.COMBAT,GLFW.GLFW_KEY_UNKNOWN); }
    @Override public void onEnable()  {}
    @Override public void onDisable() {}

    public float getEffectiveReach() {
        if (!isEnabled()) return 3.0f;
        if (bypass.is("Grim")) return CAP_GRIM_MIN + rng.nextFloat() * (CAP_GRIM_MAX - CAP_GRIM_MIN);
        float cap = switch (bypass.getValue()) {
            case "Vulcan" -> CAP_VULCAN; case "Polar" -> CAP_POLAR; default -> CAP_UNIVERSAL;
        };
        float result = Math.min(dist.getValue(), cap);
        if (randomize.getValue()) result = Math.min(result + (rng.nextFloat() - 0.5f) * 2 * randAmt.getValue(), cap);
        return Math.max(3.0f, result);
    }
}
