package com.killaura.settings;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }
    public void toggle() { setValue(!getValue()); }
    @Override
    public String getDisplayValue() { return getValue() ? "§aOn" : "§cOff"; }
}
