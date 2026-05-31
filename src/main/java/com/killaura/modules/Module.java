package com.killaura.modules;

import com.killaura.settings.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {

    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private final KeyBinding keyBinding;
    protected final List<Setting<?>> settings = new ArrayList<>();

    public enum Category {
        COMBAT("⚔ Combat"), MOVEMENT("🏃 Movement"), VISUAL("👁 Visual"), MISC("⚙ Misc");
        public final String display;
        Category(String display) { this.display = display; }
    }

    public Module(String name, String description, Category category, int defaultKey) {
        this.name = name; this.description = description; this.category = category; this.enabled = false;
        this.keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.killaura-client." + name.toLowerCase().replace(" ", "_"),
                InputUtil.Type.KEYSYM, defaultKey, "category.killaura-client.modules"));
    }

    protected BooleanSetting bool(String name, String desc, boolean def) {
        BooleanSetting s = new BooleanSetting(name, desc, def); settings.add(s); return s;
    }
    protected FloatSetting flt(String name, String desc, float def, float min, float max, float step) {
        FloatSetting s = new FloatSetting(name, desc, def, min, max, step); settings.add(s); return s;
    }
    protected IntSetting intg(String name, String desc, int def, int min, int max) {
        IntSetting s = new IntSetting(name, desc, def, min, max); settings.add(s); return s;
    }
    protected ModeSetting mode(String name, String desc, String def, String... modes) {
        ModeSetting s = new ModeSetting(name, desc, def, modes); settings.add(s); return s;
    }

    public abstract void onEnable();
    public abstract void onDisable();
    public void onTick() {}
    public void toggle() { setEnabled(!enabled); }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getDisplayName() { return name; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public KeyBinding getKeyBinding() { return keyBinding; }
    public List<Setting<?>> getSettings() { return settings; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();
    }
}
