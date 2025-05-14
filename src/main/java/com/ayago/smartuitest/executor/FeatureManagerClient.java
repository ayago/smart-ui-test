package com.ayago.smartuitest.executor;

import com.ayago.smartuitest.testscenario.TestScenario;
import com.ayago.smartuitest.testscenario.TestScenario.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class FeatureManagerClient {
    private final CacheManager cacheManager;
    
    @Autowired
    public FeatureManagerClient(CacheManager cacheManager){
        this.cacheManager = cacheManager;
    }
    

    public void applyFeatureFlags(Map<String, TestScenario.Feature> flags){
        for (Map.Entry<String, Feature> entry : flags.entrySet()){
            Feature flag = entry.getValue();
            System.out.println(
                "[FeatureManager] Setting " + entry.getKey() + " to " + flag.isEnable() + " with context " + flag.getContext());
            // Simulate API call to set feature entry
        }
        cacheManager.clear();
    }
}
