package com.ayago.smartuitest.executor;

import org.springframework.stereotype.Component;

@Component
class CacheManager{

    public void clear(){
        System.out.println("[CacheManager] Clearing cache...");
        // Simulate API call to clear cache
    }
}
