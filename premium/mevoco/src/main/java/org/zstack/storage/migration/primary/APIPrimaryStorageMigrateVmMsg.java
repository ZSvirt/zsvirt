package org.zstack.storage.migration.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.MigrateVmMessage;
import org.zstack.header.vm.SkipVmTracer;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.StorageMigrationMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIPrimaryStorageMigrateVmEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
@SkipVmTracer(replyClass = APIPrimaryStorageMigrateVmEvent.class)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid", updateOnFailure = true)
public class APIPrimaryStorageMigrateVmMsg extends APIMessage implements StorageMigrationMessage, APIMultiAuditor,
        VmInstanceMessage, MigrateVmMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(resourceType = PrimaryStorageVO.class)
    private String dstPrimaryStorageUuid;

    @APIParam(required = false)
    private String dstHostUuid;

    @APIParam(required = false)
    private boolean withDataVolumes = true;

    @APIParam(required = false)
    private boolean withSnapshots = true;

    @APIParam(required = false)
    private Integer downTime;

    @APIParam(required = false, validValues = {"auto-converge"})
    private String strategy;

    @APIParam(required = false)
    private long bandwidth;

    public APIPrimaryStorageMigrateVmMsg(PrimaryStorageMigrateVmMsg pmsg) {
        vmInstanceUuid = pmsg.getVmInstanceUuid();
        dstPrimaryStorageUuid = pmsg.getDstPrimaryStorageUuid();
        dstHostUuid = pmsg.getDstHostUuid();
        withDataVolumes = pmsg.isWithDataVolumes();
        withSnapshots = pmsg.isWithSnapshots();
        downTime = pmsg.getDownTime();
    }

    public APIPrimaryStorageMigrateVmMsg() {
    }

    public static APIPrimaryStorageMigrateVmMsg __example__() {
        APIPrimaryStorageMigrateVmMsg msg = new APIPrimaryStorageMigrateVmMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setWithDataVolumes(true);
        msg.setWithSnapshots(false);
        msg.setDstPrimaryStorageUuid(uuid());
        msg.setDstHostUuid(uuid());
        msg.setDownTime(300);
        return msg;
    }

    public Integer getDownTime() {
        return downTime;
    }

    public void setDownTime(Integer downTime) {
        this.downTime = downTime;
    }

    public String getDstHostUuid() { return dstHostUuid; }

    public void setDstHostUuid(String dstHostUuid) {
        this.dstHostUuid = dstHostUuid;
    }

    public String getDstPrimaryStorageUuid() {
        return dstPrimaryStorageUuid;
    }

    public void setDstPrimaryStorageUuid(String dstPrimaryStorageUuid) {
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
    }

    public boolean isWithDataVolumes() {
        return withDataVolumes;
    }

    public void setWithDataVolumes(boolean withDataVolumes) {
        this.withDataVolumes = withDataVolumes;
    }

    public boolean isWithSnapshots() {
        return withSnapshots;
    }

    public void setWithSnapshots(boolean withSnapshots) {
        this.withSnapshots = withSnapshots;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public long getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(long bandwidth) {
        this.bandwidth = bandwidth;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        if (!rsp.isSuccess()) {
            return null;
        }

        List<APIAuditor.Result> res = new ArrayList<>();
        APIPrimaryStorageMigrateVmEvent evt = (APIPrimaryStorageMigrateVmEvent) rsp;
        /* list in result maybe a vm inventory or a errorCode */
        evt.getInventory().getAllVolumes().stream().filter(i -> i.getUuid() != null)
                .forEach(i -> res.add(new APIAuditor.Result(i.getUuid(), VolumeVO.class)));
        res.add(new APIAuditor.Result(evt.getInventory().getUuid(), VmInstanceVO.class));
        return res;
    }

    @Override
    public String getHostUuid() {
        return dstHostUuid;
    }

    @Override
    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    @Override
    public boolean isMigrateFromDestination() {
        return false;
    }

    @Override
    public boolean isAllowUnknown() {
        return false;
    }
}
