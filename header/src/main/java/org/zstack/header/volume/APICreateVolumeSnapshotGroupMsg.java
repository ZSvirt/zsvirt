package org.zstack.header.volume;

import org.springframework.http.HttpMethod;

import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.ConsistentType;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by MaJin on 2019/7/9.
 */
@RestRequest(
        path = "/volume-snapshots/group",
        method = HttpMethod.POST,
        responseClass = APICreateVolumeSnapshotGroupEvent.class,
        parameterName = "params"
)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VolumeUuidToVmUuidResolver", field = "rootVolumeUuid", updateOnFailure = true)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APICreateVolumeSnapshotGroupMsg extends APICreateMessage implements VolumeMessage, CreateVolumeSnapshotGroupMessage, APIMultiAuditor {
    /**
     * @desc root volume uuid. See :ref:`VolumeInventory`
     */
    @APIParam(resourceType = VolumeVO.class)
    private String rootVolumeUuid;
    /**
     * @desc snapshot group name. Max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc snapshot group description. Max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false)
    private boolean withMemory = false;

    @APINoSee
    private ConsistentType consistentType = ConsistentType.None;

    @APINoSee
    private VmInstanceInventory vmInstance;

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getVolumeUuid() {
        return rootVolumeUuid;
    }

    @Override
    public VmInstanceInventory getVmInstance() {
        return vmInstance;
    }

    @Override
    public void setVmInstance(VmInstanceInventory vmInstance) {
        this.vmInstance = vmInstance;
    }

    public boolean isWithMemory() {
        return withMemory;
    }

    public void setWithMemory(boolean withMemory) {
        this.withMemory = withMemory;
    }

    public static APICreateVolumeSnapshotGroupMsg __example__() {
        APICreateVolumeSnapshotGroupMsg result = new APICreateVolumeSnapshotGroupMsg();
        result.name = "test";
        result.rootVolumeUuid = uuid();
        return result;
    }

    public ConsistentType getConsistentType() {
        return consistentType;
    }

    public void setConsistentType(ConsistentType consistentType) {
        this.consistentType = consistentType;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        APICreateVolumeSnapshotGroupMsg cmsg = (APICreateVolumeSnapshotGroupMsg) msg;
        List<APIAuditor.Result> res = new ArrayList<>();
        if (rsp.isSuccess()) {
            res.add(new APIAuditor.Result(((APICreateVolumeSnapshotGroupEvent) rsp).getInventory().getUuid(), VolumeSnapshotGroupVO.class));

            List<VolumeSnapshotGroupRefInventory> volumeSnapshotRefs = ((APICreateVolumeSnapshotGroupEvent) rsp).getInventory().getVolumeSnapshotRefs();
            volumeSnapshotRefs.forEach(it -> {
                res.add(new APIAuditor.Result(it.getVolumeSnapshotUuid(), VolumeSnapshotVO.class));
            });
        }

        res.add(new APIAuditor.Result(cmsg.getVmInstance().getUuid(), VmInstanceVO.class));
        return res;
    }
}
