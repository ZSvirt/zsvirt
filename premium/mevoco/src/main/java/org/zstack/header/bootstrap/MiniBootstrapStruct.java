package org.zstack.header.bootstrap;

public class MiniBootstrapStruct {
    private String role;
    private String jobUuid;
    private MiniHostInfo local;
    private MiniHostInfo peer;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public MiniHostInfo getLocal() {
        return local;
    }

    public void setLocal(MiniHostInfo local) {
        this.local = local;
    }

    public MiniHostInfo getPeer() {
        return peer;
    }

    public void setPeer(MiniHostInfo peer) {
        this.peer = peer;
    }
}
