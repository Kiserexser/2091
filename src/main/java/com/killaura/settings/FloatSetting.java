package com.killaura.settings;

public class FloatSetting extends Setting<Float> {
    private final float min, max, step;

    public FloatSetting(String name, String description, float defaultValue, float min, float max, float step) {
        super(name, description, defaultValue);
        this.min = min; this.max = max; this.step = step;
    }

    public float getMin() { return min; }
    public float getMax() { return max; }
    public float getStep() { return step; }

    @Override
    public void setValue(Float value) { super.setValue(Math.max(min, Math.min(max, value))); }

    public float getNormalized() { return (getValue() - min) / (max - min); }

    public void setNormalized(float t) {
        float raw = min + t * (max - min);
        setValue(Math.round(raw / step) * step);
    }

    @Override
    public String getDisplayValue() { return String.format("%.2f", getValue()); }
}
