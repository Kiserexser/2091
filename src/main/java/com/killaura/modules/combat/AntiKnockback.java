package com.killaura.modules.combat;

import com.killaura.modules.Module;
import com.killaura.settings.*;
import org.lwjgl.glfw.GLFW;

public class AntiKnockback extends Module {
    public final FloatSetting horizontal = flt("Horizontal","H knockback multiplier",0.0f,0.0f,1.0f,0.05f);
    public final FloatSetting vertical   = flt("Vertical",  "V knockback multiplier",0.0f,0.0f,1.0f,0.05f);
    public final ModeSetting  mode       = mode("Mode","Bypass mode","Grim","Grim","Vulcan","Polar","Matrix","None");
    public final FloatSetting cancelRate = flt("CancelRate","Chance to cancel (0–1)",1.0f,0.0f,1.0f,0.05f);

    public AntiKnockback() { super("AntiKnockback","Reduces knockback from hits",Category.COMBAT,GLFW.GLFW_KEY_UNKNOWN); }
    @Override public void onEnable()  {}
    @Override public void onDisable() {}

    public double[] process(double x, double y, double z) {
        if (!isEnabled()) return new double[]{x, y, z};
        if (Math.random() > cancelRate.getValue()) return new double[]{x, y, z};
        return switch (mode.getValue()) {
            case "Grim"   -> new double[]{ x*0.08, y*vertical.getValue(), z*0.08 };
            case "Vulcan" -> new double[]{ x*0.12, y*vertical.getValue(), z*0.12 };
            case "Polar"  -> new double[]{ x*0.10, y*vertical.getValue(), z*0.10 };
            default       -> new double[]{ x*horizontal.getValue(), y*vertical.getValue(), z*horizontal.getValue() };
        };
    }
}
