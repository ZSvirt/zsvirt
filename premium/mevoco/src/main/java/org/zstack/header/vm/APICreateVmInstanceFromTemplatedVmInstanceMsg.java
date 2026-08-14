package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.VmCustomSpecificationStruct;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.header.zone.ZoneVO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

@TagResourceType(VmInstanceVO.class)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 36)
@RestRequest(
        path = "/vm-instances/{templatedVmInstanceUuid}/create-vmInstance-from-templated-vmInstance",
        method = HttpMethod.POST,
        responseClass = APICreateVmInstanceFromTemplatedVmInstanceEvent.class,
        parameterName = "params"
)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VmUuidDirectResolver", field = "templatedVmInstanceUuid", updateOnFailure = true)
public class APICreateVmInstanceFromTemplatedVmInstanceMsg extends APIMessage implements APIMultiAuditor, NewVmInstanceMessage2, APIBatchRequest {
    @APIParam(nonempty = true)
    private List<String> names;

    @APIParam(resourceType = TemplatedVmInstanceVO.class)
    private String templatedVmInstanceUuid;

    @APIParam(required = false, validValues = {"InstantStart", "JustCreate", "CreateStopped"})
    private String strategy = VmCreationStrategy.InstantStart.toString();

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false)
    private Integer cpuNum;

    @APIParam(required = false)
    private Long memorySize;

    @APIParam(required = false, numberRange = {0, Long.MAX_VALUE})
    private Long reservedMemorySize;

    @APIParam(required = false, resourceType = L3NetworkVO.class)
    private List<String> l3NetworkUuids;

    @APIParam(required = false, resourceType = L3NetworkVO.class, emptyString = false)
    private String defaultL3NetworkUuid;

    @APIParam(required = false)
    private String vmNicParams;

    @APIParam(required = false)
    private List<DiskAO> diskAOs;

    @APIParam(required = false, resourceType = ZoneVO.class)
    private String zoneUuid;

    @APIParam(required = false, resourceType = ClusterVO.class)
    private String clusterUuid;

    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(required = false)
    private String instanceOfferingUuid;

    @APIParam(required = false)
    private String type;

    @APIParam(required = false, scope = APIParam.SCOPE_ALLOWED_SHARING)
    private VmCustomSpecificationStruct vmCustomSpecification;

    @APINoSee
    private VmInstanceVO templatedVmInstance;

    @APINoSee
    private TemplatedVmInstanceCacheVO templatedVmInstanceCache;

    @APINoSee
    private VolumeSnapshotGroupInventory templatedCacheVolumeSnapshotGroup;

    @APINoSee
    List<String> rootSystemTags;

    @APIParam(required = false)
    private Boolean resetTpm;

    @APINoSee
    private String platform;

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public String getTemplatedVmInstanceUuid() {
        return templatedVmInstanceUuid;
    }

    public void setTemplatedVmInstanceUuid(String templatedVmInstanceUuid) {
        this.templatedVmInstanceUuid = templatedVmInstanceUuid;
    }

    @Override
    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Integer getCpuNum() {
        return cpuNum;
    }

    @Override
    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
    }

    @Override
    public Long getMemorySize() {
        return memorySize;
    }

    @Override
    public void setMemorySize(Long memorySize) {
        this.memorySize = memorySize;
    }

    @Override
    public Long getReservedMemorySize() {
        return reservedMemorySize;
    }

    @Override
    public void setReservedMemorySize(Long reservedMemorySize) {
        this.reservedMemorySize = reservedMemorySize;
    }

    @Override
    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    @Override
    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    @Override
    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    @Override
    public String getVmNicParams() {
        return vmNicParams;
    }

    public void setVmNicParams(String vmNicParams) {
        this.vmNicParams = vmNicParams;
    }

    public List<DiskAO> getDiskAOs() {
        return diskAOs;
    }

    public void setDiskAOs(List<DiskAO> diskAOs) {
        this.diskAOs = diskAOs;
    }

    @Override
    public String getZoneUuid() {
        return zoneUuid;
    }

    @Override
    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    @Override
    public String getClusterUuid() {
        return clusterUuid;
    }

    @Override
    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    public VmCustomSpecificationStruct getVmCustomSpecification() {
        return vmCustomSpecification;
    }

    public void setVmCustomSpecification(VmCustomSpecificationStruct vmCustomSpecification) {
        this.vmCustomSpecification = vmCustomSpecification;
    }

    @Override
    public String getName() {
        return getNames().get(0);
    }

    public VmInstanceVO getTemplatedVmInstance() {
        return templatedVmInstance;
    }

    public void setTemplatedVmInstance(VmInstanceVO templatedVmInstance) {
        this.templatedVmInstance = templatedVmInstance;
    }

    public TemplatedVmInstanceCacheVO getTemplatedVmInstanceCache() {
        return templatedVmInstanceCache;
    }

    public void setTemplatedVmInstanceCache(TemplatedVmInstanceCacheVO templatedVmInstanceCache) {
        this.templatedVmInstanceCache = templatedVmInstanceCache;
    }

    public VolumeSnapshotGroupInventory getTemplatedCacheVolumeSnapshotGroup() {
        return templatedCacheVolumeSnapshotGroup;
    }

    public void setTemplatedCacheVolumeSnapshotGroup(VolumeSnapshotGroupInventory templatedCacheVolumeSnapshotGroup) {
        this.templatedCacheVolumeSnapshotGroup = templatedCacheVolumeSnapshotGroup;
    }

    public List<String> getRootSystemTags() {
        return rootSystemTags;
    }

    public void setRootSystemTags(List<String> rootSystemTags) {
        this.rootSystemTags = rootSystemTags;
    }

    public Boolean getResetTpm() {
        return resetTpm;
    }

    public void setResetTpm(Boolean resetTpm) {
        this.resetTpm = resetTpm;
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }


    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        if (!rsp.isSuccess()) {
            return null;
        }

        List<APIAuditor.Result> res = new ArrayList<>();
        APICreateVmInstanceFromTemplatedVmInstanceEvent evt = (APICreateVmInstanceFromTemplatedVmInstanceEvent) rsp;

        evt.getResult().getInventories().stream().filter(i -> i.getInventory() != null)
                .forEach(i -> res.add(new APIAuditor.Result(i.getInventory().getUuid(), VmInstanceVO.class)));
        return res;
    }

    public static APICreateVmInstanceFromTemplatedVmInstanceMsg __example__() {
        APICreateVmInstanceFromTemplatedVmInstanceMsg msg = new APICreateVmInstanceFromTemplatedVmInstanceMsg();
        msg.setNames(list("vm1", "vm2"));
        msg.setTemplatedVmInstanceUuid(uuid(TemplatedVmInstanceVO.class));
        return msg;
    }

    @Override
    public APIBatchRequest.Result collectResult(APIMessage message, APIEvent rsp) {
        APICreateVmInstanceFromTemplatedVmInstanceEvent evt = (APICreateVmInstanceFromTemplatedVmInstanceEvent) rsp;
        return new APIBatchRequest.Result(
                evt.getResult().getInventories().size(),
                evt.getResult().getInventoriesWithoutError().size()
        );
    }
}