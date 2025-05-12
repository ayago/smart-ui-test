package com.ayago.pageflow;

import java.util.List;
import java.util.Objects;

public class TestScenario{
    private final String host;
    private final List<Feature> features;
    private final List<Page> pages;
    
    public TestScenario(String host, List<Feature> features, List<Page> pages) {
        this.host = host;
        this.features = features;
        this.pages = pages;
    }
    
    
    public String getHost() {
        return host;
    }
    
    public List<Feature> getFeatures() {
        return features;
    }
    
    public List<Page> getPages() {
        return pages;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestScenario that = (TestScenario) o;
        return Objects.equals(host, that.host) && Objects.equals(features, that.features) && Objects.equals(pages, that.pages);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(host, features, pages);
    }
}
