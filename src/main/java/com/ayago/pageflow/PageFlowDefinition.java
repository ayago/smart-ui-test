package com.ayago.pageflow;

import java.util.List;
import java.util.Objects;

public class PageFlowDefinition {
    private final String host;
    private final List<FeatureFlag> featureFlags;
    private final List<PageModel> pages;
    
    public PageFlowDefinition(String host, List<FeatureFlag> featureFlags, List<PageModel> pages) {
        this.host = host;
        this.featureFlags = featureFlags;
        this.pages = pages;
    }
    
    
    public String getHost() {
        return host;
    }
    
    public List<FeatureFlag> getFeatureFlags() {
        return featureFlags;
    }
    
    public List<PageModel> getPages() {
        return pages;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageFlowDefinition that = (PageFlowDefinition) o;
        return Objects.equals(host, that.host) && Objects.equals(featureFlags, that.featureFlags) && Objects.equals(pages, that.pages);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(host, featureFlags, pages);
    }
}
