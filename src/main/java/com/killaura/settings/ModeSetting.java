package com.killaura.settings;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultMode, String... modes) {
        super(name, description, defaultMode);
        this.modes = Arrays.asList(modes);
    }

    public List<String> getModes() { return modes; }
    public void cycle() { int idx = modes.indexOf(getValue()); setValue(modes.get((idx + 1) % modes.size())); }
    public boolean is(String mode) { return getValue().equalsIgnoreCase(mode); }

    @Override
    public String getDisplayValue() { return getValue(); }
}
