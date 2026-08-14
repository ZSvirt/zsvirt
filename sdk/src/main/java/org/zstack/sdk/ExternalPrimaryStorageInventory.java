package org.zstack.sdk;



public class ExternalPrimaryStorageInventory extends org.zstack.sdk.PrimaryStorageInventory {

    public java.lang.String identity;
    public void setIdentity(java.lang.String identity) {
        this.identity = identity;
    }
    public java.lang.String getIdentity() {
        return this.identity;
    }

    public java.util.LinkedHashMap config;
    public void setConfig(java.util.LinkedHashMap config) {
        this.config = config;
    }
    public java.util.LinkedHashMap getConfig() {
        return this.config;
    }

    public java.util.LinkedHashMap addonInfo;
    public void setAddonInfo(java.util.LinkedHashMap addonInfo) {
        this.addonInfo = addonInfo;
    }
    public java.util.LinkedHashMap getAddonInfo() {
        return this.addonInfo;
    }

    public java.util.List outputProtocols;
    public void setOutputProtocols(java.util.List outputProtocols) {
        this.outputProtocols = outputProtocols;
    }
    public java.util.List getOutputProtocols() {
        return this.outputProtocols;
    }

    public java.lang.String defaultProtocol;
    public void setDefaultProtocol(java.lang.String defaultProtocol) {
        this.defaultProtocol = defaultProtocol;
    }
    public java.lang.String getDefaultProtocol() {
        return this.defaultProtocol;
    }

}
