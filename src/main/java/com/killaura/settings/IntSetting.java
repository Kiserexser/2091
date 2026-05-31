package com.killaura.settings;

public class IntSetting extends Setting<Integer> {
    private final int min, max;

    public IntSetting(String name, String description, int defaultValue, int min, int max) {
        super(name, description, defaultValue);
        this.min = min; this.max = max;
    }

    public int getMin() { return min; }
    public int getMax() { return max; }

    @Override
    public void setValue(Integer value) { super.setValue(Math.max(min, Math.min(max, value))); }

    public float getNormalized() { return (float)(getValue() - min) / (max - min); }
    public void setNormalized(float t) { setValue(min + Math.round(t * (max - min))); }

    @Override
    public String getDisplayValue() { return String.valueOf(getValue()); }
}
