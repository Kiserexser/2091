package com.killaura.modules.movement;

import com.killaura.modules.Module;
import com.killaura.settings.*;
import org.lwjgl.glfw.GLFW;
import java.util.Random;

public class Velocity extends Module {
    public final FloatSetting horizontal = flt("Horizontal","H-velocity multiplier",0.0f,0.0f,1.0f,0.05f);
    public final FloatSetting vertical   = flt("Vertical",  "V-velocity multiplier",0.0f,0.0f,1.0f,0.05f);
    public final ModeSetting  bypass     = mode("Bypass","AC bypass","Grim","Grim","Vulcan","Polar","Matrix","Spartan","None");
    public final FloatSetting cancelRate = flt("CancelRate","Probability to cancel",1.0f,0.0f,1.0f,0.05f);
    private final Random rng = new Random();

    public Velocity() { super("Velocity","Cancels incoming knockback",Category.MOVEMENT,GLFW.GLFW_KEY_UNKNOWN); }
    @Override public void onEnable()  { **...**

_This response is too long to display in full._
