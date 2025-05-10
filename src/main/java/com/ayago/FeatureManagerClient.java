package com.ayago;

import com.ayago.pageflow.FeatureFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class FeatureManagerClient {
    private final CacheManagerImpl cacheManager;
    
    @Autowired
    public FeatureManagerClient(CacheManagerImpl cacheManager){
        this.cacheManager = cacheManager;
    }
    

    public void applyFeatureFlags(List<FeatureFlag> flags){
        for (FeatureFlag flag : flags){
            System.out.println(
                "[FeatureManager] Setting " + flag.getName() + " to " + flag.isEnabled() + " with context " + flag.getContext());
            // Simulate API call to set feature flag
        }
        cacheManager.clear();
    }
}
