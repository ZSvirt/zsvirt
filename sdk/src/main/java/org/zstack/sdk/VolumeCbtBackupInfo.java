package org.zstack.sdk;

import org.zstack.sdk.VolumeTO;

public class VolumeCbtBackupInfo  {

    public VolumeTO volume;
    public void setVolume(VolumeTO volume) {
        this.volume = volume;
    }
    public VolumeTO getVolume() {
        return this.volume;
    }

    public java.lang.String bitmapBase64;
    public void setBitmapBase64(java.lang.String bitmapBase64) {
        this.bitmapBase64 = bitmapBase64;
    }
    public java.lang.String getBitmapBase64() {
        return this.bitmapBase64;
    }

    public java.lang.String target;
    public void setTarget(java.lang.String target) {
        this.target = target;
    }
    public java.lang.String getTarget() {
        return this.target;
    }

    public java.lang.String scratchNodeName;
    public void setScratchNodeName(java.lang.String scratchNodeName) {
        this.scratchNodeName = scratchNodeName;
    }
    public java.lang.String getScratchNodeName() {
        return this.scratchNodeName;
    }

    public java.lang.String metadata;
    public void setMetadata(java.lang.String metadata) {
        this.metadata = metadata;
    }
    public java.lang.String getMetadata() {
        return this.metadata;
    }

    public java.lang.Long nbdPort;
    public void setNbdPort(java.lang.Long nbdPort) {
        this.nbdPort = nbdPort;
    }
    public java.lang.Long getNbdPort() {
        return this.nbdPort;
    }

    public java.lang.String nbdServer;
    public void setNbdServer(java.lang.String nbdServer) {
        this.nbdServer = nbdServer;
    }
    public java.lang.String getNbdServer() {
        return this.nbdServer;
    }

    public java.lang.String mode;
    public void setMode(java.lang.String mode) {
        this.mode = mode;
    }
    public java.lang.String getMode() {
        return this.mode;
    }

    public java.lang.String bitmapName;
    public void setBitmapName(java.lang.String bitmapName) {
        this.bitmapName = bitmapName;
    }
    public java.lang.String getBitmapName() {
        return this.bitmapName;
    }

}
