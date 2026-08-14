package org.zstack.sdk;



public class VmCapabilities  {

    public boolean supportLiveMigration;
    public void setSupportLiveMigration(boolean supportLiveMigration) {
        this.supportLiveMigration = supportLiveMigration;
    }
    public boolean getSupportLiveMigration() {
        return this.supportLiveMigration;
    }

    public boolean supportVolumeMigration;
    public void setSupportVolumeMigration(boolean supportVolumeMigration) {
        this.supportVolumeMigration = supportVolumeMigration;
    }
    public boolean getSupportVolumeMigration() {
        return this.supportVolumeMigration;
    }

    public boolean supportReimage;
    public void setSupportReimage(boolean supportReimage) {
        this.supportReimage = supportReimage;
    }
    public boolean getSupportReimage() {
        return this.supportReimage;
    }

    public boolean supportMemorySnapshot;
    public void setSupportMemorySnapshot(boolean supportMemorySnapshot) {
        this.supportMemorySnapshot = supportMemorySnapshot;
    }
    public boolean getSupportMemorySnapshot() {
        return this.supportMemorySnapshot;
    }

}
