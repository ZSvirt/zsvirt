package org.zstack.sdk;



public class NkpInventory extends org.zstack.sdk.KeyProviderInventory {

    public java.lang.String kdf;
    public void setKdf(java.lang.String kdf) {
        this.kdf = kdf;
    }
    public java.lang.String getKdf() {
        return this.kdf;
    }

    public java.lang.String saltPolicy;
    public void setSaltPolicy(java.lang.String saltPolicy) {
        this.saltPolicy = saltPolicy;
    }
    public java.lang.String getSaltPolicy() {
        return this.saltPolicy;
    }

    public boolean backedUp;
    public void setBackedUp(boolean backedUp) {
        this.backedUp = backedUp;
    }
    public boolean getBackedUp() {
        return this.backedUp;
    }

    public java.lang.Integer currentVersion;
    public void setCurrentVersion(java.lang.Integer currentVersion) {
        this.currentVersion = currentVersion;
    }
    public java.lang.Integer getCurrentVersion() {
        return this.currentVersion;
    }

}
