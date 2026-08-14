package org.zstack.sdk;



public class VolumeMigrationAO  {

    public java.lang.String volumeUuid;
    public void setVolumeUuid(java.lang.String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
    public java.lang.String getVolumeUuid() {
        return this.volumeUuid;
    }

    public java.lang.String dstPrimaryStorageUuid;
    public void setDstPrimaryStorageUuid(java.lang.String dstPrimaryStorageUuid) {
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
    }
    public java.lang.String getDstPrimaryStorageUuid() {
        return this.dstPrimaryStorageUuid;
    }

    public boolean withSnapshots;
    public void setWithSnapshots(boolean withSnapshots) {
        this.withSnapshots = withSnapshots;
    }
    public boolean getWithSnapshots() {
        return this.withSnapshots;
    }

}
