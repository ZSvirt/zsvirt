package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.VmCustomSpecificationStruct;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIBatchRequest;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by david on 7/30/16.
 */
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 36)
@RestRequest(path = "/vm-instances/{vmInstanceUuid}/actions", method = HttpMethod.PUT, responseClass = APICloneVmInstanceEvent.class, isAction = true)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid", updateOnFailure = true)
public class APICloneVmInstanceMsg extends APIMessage implements APIMultiAuditor, APIBatchRequest {

    /**
     * @desc: the uuid of the VM instance to be cloned
     */
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(required = false, validValues = { "InstantStart", "JustCreate", "CreateStopped" })
    private String strategy = VmCreationStrategy.InstantStart.toString();

    @APIParam(required = false)
    private String vmNicParams;

    /**
     * @desc max length of 255 characters
     */
    @APIParam(nonempty = true)
    private List<String> names;

    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuidForRootVolume;

    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuidForDataVolume;

    private Boolean full = false;

    @APIParam(required = false)
    private List<String> rootVolumeSystemTags;

    @APIParam(required = false)
    private List<String> dataVolumeSystemTags;

    @APIParam(required = false, resourceType = ClusterVO.class)
    private String clusterUuid;

    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(required = false)
    private List<DiskAO> diskAOs;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false, scope = APIParam.SCOPE_ALLOWED_SHARING)
    private VmCustomSpecificationStruct vmCustomSpecification;

    @APIParam(required = false)
    private Boolean resetTpm;

    @APINoSee
    private VmInstanceInventory vmInstanceInventory;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
    }

    public String getPrimaryStorageUuidForDataVolume() {
        return primaryStorageUuidForDataVolume;
    }

    public void setPrimaryStorageUuidForDataVolume(String primaryStorageUuidForDataVolume) {
        this.primaryStorageUuidForDataVolume = primaryStorageUuidForDataVolume;
    }

    public List<String> getRootVolumeSystemTags() {
        return rootVolumeSystemTags;
    }

    public void setRootVolumeSystemTags(List<String> rootVolumeSystemTags) {
        this.rootVolumeSystemTags = rootVolumeSystemTags;
    }

    public List<String> getDataVolumeSystemTags() {
        return dataVolumeSystemTags;
    }

    public void setDataVolumeSystemTags(List<String> dataVolumeSystemTags) {
        this.dataVolumeSystemTags = dataVolumeSystemTags;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getVmNicParams() {
        return vmNicParams;
    }

    public void setVmNicParams(String vmNicParams) {
        this.vmNicParams = vmNicParams;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<DiskAO> getDiskAOs() {
        return diskAOs;
    }

    public void setDiskAOs(List<DiskAO> diskAOs) {
        this.diskAOs = diskAOs;
    }

    public VmInstanceInventory getVmInstanceInventory() {
        return vmInstanceInventory;
    }

    public void setVmInstanceInventory(VmInstanceInventory vmInstanceInventory) {
        this.vmInstanceInventory = vmInstanceInventory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VmCustomSpecificationStruct getVmCustomSpecification() {
        return vmCustomSpecification;
    }

    public void setVmCustomSpecification(VmCustomSpecificationStruct vmCustomSpecification) {
        this.vmCustomSpecification = vmCustomSpecification;
    }

    public Boolean getResetTpm() {
        return resetTpm;
    }

    public void setResetTpm(Boolean resetTpm) {
        this.resetTpm = resetTpm;
    }

    public static APICloneVmInstanceMsg __example__() {
        APICloneVmInstanceMsg msg = new APICloneVmInstanceMsg();
        msg.setNames(list("vm1", "vm2"));
        msg.setStrategy("InstantStart");
        msg.setVmInstanceUuid(uuid());
        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        if (!rsp.isSuccess())  {
            return null;
        }

        List<APIAuditor.Result> res = new ArrayList<>();
        APICloneVmInstanceEvent evt = (APICloneVmInstanceEvent) rsp;
        /* list in result maybe a vm inventory or a errorCode */
        evt.getResult().getInventories().stream().filter(i -> i.getInventory() != null)
                .forEach(i -> res.add(new APIAuditor.Result(i.getInventory().getUuid(), VmInstanceVO.class)));
        return res;
    }

    public Boolean getFull() {
        return full;
    }

    public void setFull(Boolean full) {
        this.full = full;
    }

    @Override
    public APIBatchRequest.Result collectResult(APIMessage message, APIEvent rsp) {
        APICloneVmInstanceEvent evt = (APICloneVmInstanceEvent) rsp;
        return new APIBatchRequest.Result(
                evt.getResult().getInventories().size(),
                evt.getResult().getInventoriesWithoutError().size()
        );
    }
}
