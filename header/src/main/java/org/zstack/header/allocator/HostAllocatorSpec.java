package org.zstack.header.allocator;

import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmLocationSpec;
import org.zstack.header.vm.VmNicParam;

import java.util.*;

/**
 */
public class HostAllocatorSpec {
    // locations
    // recommendCluster, avoidHostUuids and softAvoidHostUuids
    private List<VmLocationSpec> locationSpecs = new ArrayList<>();
    private String zoneUuid;
    private List<String> clusterUuids;
    private String hostUuid;

    // matched properties
    private long cpuCapacity;
    private long memoryCapacity;
    private List<String> l3NetworkUuids;
    private List<VmNicParam> vmNicParams = new ArrayList<>();
    private long diskSize;
    private String hypervisorType;
    private String allocatorStrategy;
    private VmInstanceInventory vmInstance;
    private ImageInventory image;
    private String imageRequiredPrimaryStorageUuid;
    private String vmOperation;
    private List<DiskOfferingInventory> diskOfferings = new ArrayList<>();
    private boolean allowNoL3Networks;
    private boolean listAllHosts;
    private String requiredBackupStorageUuid;
    // each primary storage in this set is required
    private Set<String> requiredPrimaryStorageUuids = new HashSet<>();
    // for each set in the list, the primary storage inside is optional
    private final List<Set<String>> optionalPrimaryStorageUuids = new ArrayList<>();
    private Map<String, List<String>> backupStoragePrimaryStorageMetrics;
    private boolean dryRun;
    private List<String> systemTags;
    private Map<String, Set<String>> host2PrimaryStorageBlacklist = new HashMap<>();
    private boolean fullAllocate;
    private long oldMemoryCapacity = 0;
    private AllocationScene allocationScene;
    private String architecture;

    public List<VmLocationSpec> getLocationSpecs() {
        return locationSpecs;
    }

    public void setLocationSpecs(List<VmLocationSpec> locationSpecs) {
        this.locationSpecs = locationSpecs;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public List<String> getClusterUuids() {
        return clusterUuids;
    }

    public void setClusterUuids(List<String> clusterUuids) {
        this.clusterUuids = clusterUuids;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public AllocationScene getAllocationScene() {
        return allocationScene;
    }

    public void setAllocationScene(AllocationScene allocationScene) {
        this.allocationScene = allocationScene;
    }

    public List<String> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<String> systemTags) {
        this.systemTags = systemTags;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public void setRequiredPrimaryStorageUuids(Set<String> requiredPrimaryStorageUuids) {
        this.requiredPrimaryStorageUuids = requiredPrimaryStorageUuids;
    }

    public Set<String> getRequiredPrimaryStorageUuids() {
        return requiredPrimaryStorageUuids;
    }

    public List<Set<String>> getOptionalPrimaryStorageUuids() {
        return optionalPrimaryStorageUuids;
    }

    public void addOptionalPrimaryStorageUuids(Set<String> optionalPrimaryStorageUuids) {
        if (optionalPrimaryStorageUuids != null) {
            this.optionalPrimaryStorageUuids.add(optionalPrimaryStorageUuids);
        }
    }

    public String getRequiredBackupStorageUuid() {
        return requiredBackupStorageUuid;
    }

    public void setRequiredBackupStorageUuid(String requiredBackupStorageUuid) {
        this.requiredBackupStorageUuid = requiredBackupStorageUuid;
    }

    public boolean isListAllHosts() {
        return listAllHosts;
    }

    public void setListAllHosts(boolean listAllHosts) {
        this.listAllHosts = listAllHosts;
    }

    public boolean isAllowNoL3Networks() {
        return allowNoL3Networks;
    }

    public void setAllowNoL3Networks(boolean allowNoL3Networks) {
        this.allowNoL3Networks = allowNoL3Networks;
    }

    public List<DiskOfferingInventory> getDiskOfferings() {
        return diskOfferings;
    }

    public void setDiskOfferings(List<DiskOfferingInventory> diskOfferings) {
        this.diskOfferings = diskOfferings;
    }

    public String getVmOperation() {
        return vmOperation;
    }

    public void setVmOperation(String vmOperation) {
        this.vmOperation = vmOperation;
    }

    public long getCpuCapacity() {
        return cpuCapacity;
    }

    public void setCpuCapacity(long cpuCapacity) {
        this.cpuCapacity = cpuCapacity;
    }

    public long getMemoryCapacity() {
        return memoryCapacity;
    }

    public void setMemoryCapacity(long memoryCapacity) {
        this.memoryCapacity = memoryCapacity;
    }

    public List<String> getL3NetworkUuids() {
        if (l3NetworkUuids == null) {
            l3NetworkUuids = new ArrayList<>();
        }
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public List<VmNicParam> getVmNicParams() {
        return vmNicParams;
    }

    public void setVmNicParams(List<VmNicParam> vmNicParams) {
        this.vmNicParams = vmNicParams;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }

    public VmInstanceInventory getVmInstance() {
        return vmInstance;
    }

    public void setVmInstance(VmInstanceInventory vmInstance) {
        this.vmInstance = vmInstance;
    }

    public ImageInventory getImage() {
        return image;
    }

    public void setImage(ImageInventory image) {
        this.image = image;
    }

    public String getImageRequiredPrimaryStorageUuid() {
        return imageRequiredPrimaryStorageUuid;
    }

    public void setImageRequiredPrimaryStorageUuid(String imageRequiredPrimaryStorageUuid) {
        this.imageRequiredPrimaryStorageUuid = imageRequiredPrimaryStorageUuid;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public static HostAllocatorSpec fromAllocationMsg(AllocateHostMsg msg) {
        HostAllocatorSpec spec = new HostAllocatorSpec();
        spec.setAllocatorStrategy(msg.getAllocatorStrategy());
        spec.setLocationSpecs(msg.getLocationSpecs());
        spec.setCpuCapacity(msg.getCpuCapacity());
        spec.setDiskSize(msg.getDiskSize());
        spec.setListAllHosts(msg.isListAllHosts());
        spec.setDryRun(msg.isDryRun());
        spec.setFullAllocate(msg.isFullAllocate());
        spec.setOldMemoryCapacity(msg.getOldMemoryCapacity());
        String hvType = null;
        if (msg.getVmInstance() != null && msg.getVmInstance().getHypervisorType() != null) {
            hvType = msg.getVmInstance().getHypervisorType();
        }
        spec.setHypervisorType(hvType);
        spec.setMemoryCapacity(msg.getMemoryCapacity());
        spec.setVmInstance(msg.getVmInstance());
        spec.setL3NetworkUuids(msg.getL3NetworkUuids());
        spec.setVmNicParams(msg.getVmNicParams());
        spec.setImage(msg.getImage());
        spec.setVmOperation(msg.getVmOperation());
        spec.setDiskOfferings(msg.getDiskOfferings());
        spec.setAllowNoL3Networks(msg.isAllowNoL3Networks());
        spec.setRequiredBackupStorageUuid(msg.getRequiredBackupStorageUuid());
        spec.setRequiredPrimaryStorageUuids(msg.getRequiredPrimaryStorageUuids());
        msg.getOptionalPrimaryStorageUuids().forEach(spec::addOptionalPrimaryStorageUuids);
        spec.setAllocationScene(msg.getAllocationScene());
        spec.setArchitecture(msg.getArchitecture());
        if (msg.getSystemTags() != null && !msg.getSystemTags().isEmpty()){
            spec.setSystemTags(new ArrayList<String>(msg.getSystemTags()));
        }
        spec.setImageRequiredPrimaryStorageUuid(msg.getImageRequiredPrimaryStorageUuid());
        return spec;
    }

    public Map<String, List<String>> getBackupStoragePrimaryStorageMetrics() {
        return backupStoragePrimaryStorageMetrics;
    }

    public void setBackupStoragePrimaryStorageMetrics(Map<String, List<String>> backupStoragePrimaryStorageMetrics) {
        this.backupStoragePrimaryStorageMetrics = backupStoragePrimaryStorageMetrics;
    }

    public Map<String, Set<String>> getHost2PrimaryStorageBlacklist() {
        return host2PrimaryStorageBlacklist;
    }

    public void setHost2PrimaryStorageBlacklist(Map<String, Set<String>> host2PrimaryStorageBlacklist) {
        this.host2PrimaryStorageBlacklist = host2PrimaryStorageBlacklist;
    }

    public boolean isFullAllocate() {
        return fullAllocate;
    }

    public void setFullAllocate(boolean fullAllocate) {
        this.fullAllocate = fullAllocate;
    }

    public long getOldMemoryCapacity() {
        return oldMemoryCapacity;
    }

    public void setOldMemoryCapacity(long oldMemoryCapacity) {
        this.oldMemoryCapacity = oldMemoryCapacity;
    }
}
