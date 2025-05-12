package com.ayago.pageflow;

import java.util.Map;
import java.util.Objects;

public class Feature{
    private final String name;
    private final boolean enabled;
    private final Map<String, String> context;
    
    public Feature(String name, boolean enabled, Map<String, String> context) {
        this.name = name;
        this.enabled = enabled;
        this.context = context;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public Map<String, String> getContext() {
        return context;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Feature that = (Feature) o;
        return enabled == that.enabled && Objects.equals(name, that.name) && Objects.equals(context, that.context);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, enabled, context);
    }
}
