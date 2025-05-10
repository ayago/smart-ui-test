package com.ayago;

import org.springframework.stereotype.Component;

@Component
class CacheManagerImpl {

    public void clear(){
        System.out.println("[CacheManager] Clearing cache...");
        // Simulate API call to clear cache
    }
}
