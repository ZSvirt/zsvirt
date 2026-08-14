package org.zstack.core.puppet;

public interface PuppetConstant {
    public String SERVICE_ID = "PuppetFacade";
    
    public static enum PuppetGlobalConfig {
        useJobQueue;
        
        public String getCategory() {
            return "Puppet";
        }
    }
}
