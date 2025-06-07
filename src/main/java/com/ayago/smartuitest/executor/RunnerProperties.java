package com.ayago.smartuitest.executor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class RunnerProperties{
    
    private final ScreenShot screenShot = new ScreenShot();
    
    public ScreenShot getScreenShot(){
        return screenShot;
    }
    
    public static class ScreenShot{
        private String folder;
        
        public String getFolder(){
            return folder;
        }
        
        public void setFolder(String folder){
            this.folder = folder;
        }
    }
}
