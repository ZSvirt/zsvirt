package org.zstack.zmigrate.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIGetZMigrateInfosReply extends APIReply {
    private String zmigrateVmInstanceStatus;
    private String version;
    private long platformsCount;
    private long gatewaysCount;
    private long migrateJobsCount;
    private long zmigrateStartTime;
    private boolean vddkUploaded;

    public String getZmigrateVmInstanceStatus() {
        return zmigrateVmInstanceStatus;
    }

    public void setZmigrateVmInstanceStatus(String zmigrateVmInstanceStatus) {
        this.zmigrateVmInstanceStatus = zmigrateVmInstanceStatus;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getPlatformsCount() {
        return platformsCount;
    }

    public void setPlatformsCount(long platformsCount) {
        this.platformsCount = platformsCount;
    }

    public long getGatewaysCount() {
        return gatewaysCount;
    }

    public void setGatewaysCount(long gatewaysCount) {
        this.gatewaysCount = gatewaysCount;
    }

    public long getMigrateJobsCount() {
        return migrateJobsCount;
    }

    public void setMigrateJobsCount(long migrateJobsCount) {
        this.migrateJobsCount = migrateJobsCount;
    }

    public long getZmigrateStartTime() {
        return zmigrateStartTime;
    }

    public void setZmigrateStartTime(long zmigrateStartTime) {
        this.zmigrateStartTime = zmigrateStartTime;
    }

    public boolean isVddkUploaded() {
        return vddkUploaded;
    }

    public void setVddkUploaded(boolean vddkUploaded) {
        this.vddkUploaded = vddkUploaded;
    }

    public static APIGetZMigrateInfosReply __example__() {
        APIGetZMigrateInfosReply reply = new APIGetZMigrateInfosReply();
        reply.setZmigrateVmInstanceStatus("Running");
        reply.setVersion("1.0.0");
        reply.setPlatformsCount(3);
        reply.setGatewaysCount(2);
        reply.setMigrateJobsCount(5);
        reply.setZmigrateStartTime(DocUtils.date);
        reply.setVddkUploaded(true);
        return reply;
    }
}
