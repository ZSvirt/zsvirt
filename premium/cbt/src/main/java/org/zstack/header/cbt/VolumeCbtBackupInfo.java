package org.zstack.header.cbt;

import org.zstack.kvm.VolumeTO;

public class VolumeCbtBackupInfo {
    private VolumeTO volume;
    private String bitmapBase64;
    private String target;
    private String scratchNodeName;
    private String metadata;
    private Long nbdPort;
    private String nbdServer;
    private String mode;
    private String bitmapName;

    public VolumeTO getVolume() {
        return volume;
    }

    public void setVolume(VolumeTO volume) {
        this.volume = volume;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getBitmapBase64() {
        return bitmapBase64;
    }

    public void setBitmapBase64(String bitmapBase64) {
        this.bitmapBase64 = bitmapBase64;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Long getNbdPort() {
        return nbdPort;
    }

    public void setNbdPort(Long nbdPort) {
        this.nbdPort = nbdPort;
    }

    public String getScratchNodeName() {
        return scratchNodeName;
    }

    public void setScratchNodeName(String scratchNodeName) {
        this.scratchNodeName = scratchNodeName;
    }

    public String getNbdServer() {
        return nbdServer;
    }

    public void setNbdServer(String nbdServer) {
        this.nbdServer = nbdServer;
    }

    public String getBitmapName() {
        return bitmapName;
    }

    public void setBitmapName(String bitmapName) {
        this.bitmapName = bitmapName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
