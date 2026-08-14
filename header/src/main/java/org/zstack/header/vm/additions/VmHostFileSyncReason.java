package org.zstack.header.vm.additions;

public enum VmHostFileSyncReason {
    PrepareRead("on prepare host file (from origin host)"),
    PrepareReRead("on prepare host file (from dest host)"),
    ResourceRelease("on release vm resource"),
    PreMigration("on pre-migration (from src host)"),
    PostMigration("on post-migration (from dest host)"),
    VmShutdown("on libvirt shutdown event"),
    PostClone("on post-clone (from dest host)"),
    Restore("restore"),
    SnapshotGroupOnlineBackup("snapshot group online backup"),
    RevertSnapshot("revert snapshot"),
    VolumeBackup("volume backup"),
    BeforeHaStart("on before HA start (from last host)"),
    PeriodicDirtyCheck("on periodic dirty check"),
    PeriodicForceSync("on periodic force sync"),
    ConventToTemplatedVM("on convent to templated VM"),
    ;

    public final String detail;

    private VmHostFileSyncReason(String detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        return detail;
    }

    public String reason() {
        return detail;
    }

    public String reason(String description) {
        return description == null || description.length() == 0 ? reason() : detail + ": " + description;
    }
}
