package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.Map;

public class MigrateVmOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    public static enum StorageMigrationPolicy {
        FullCopy,
        IncCopy
    }

    private VmInstanceInventory vmInventory;
    private HostInventory destHostInventory;
    private String srcHostUuid;
    private String strategy;
    private StorageMigrationPolicy storageMigrationPolicy;
    private boolean migrateFromDestination;
    // A map from old disk to new disk
    private Map<String, String> diskMigrationMap;
    private boolean reload;
    private Integer downTime;
    private long bandwidth;

    public Integer getDownTime() {
        return downTime;
    }

    public void setDownTime(Integer downTime) {
        this.downTime = downTime;
    }

    public boolean isReload() {
        return reload;
    }

    public void setReload(boolean reload) {
        this.reload = reload;
    }

    public StorageMigrationPolicy getStorageMigrationPolicy() {
        return storageMigrationPolicy;
    }

    public void setStorageMigrationPolicy(StorageMigrationPolicy storageMigrationPolicy) {
        this.storageMigrationPolicy = storageMigrationPolicy;
    }

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }

    public HostInventory getDestHostInventory() {
        return destHostInventory;
    }

    public void setDestHostInventory(HostInventory destHostInventory) {
        this.destHostInventory = destHostInventory;
    }

    @Override
    public String getHostUuid() {
        return migrateFromDestination ? destHostInventory.getUuid() : srcHostUuid;
    }

    public String getSrcHostUuid() {
        return srcHostUuid;
    }

    public void setSrcHostUuid(String srcHostUuid) {
        this.srcHostUuid = srcHostUuid;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public boolean isMigrateFromDestination() {
        return migrateFromDestination;
    }

    public void setMigrateFromDestination(boolean migrateFromDestination) {
        this.migrateFromDestination = migrateFromDestination;
    }

    public Map<String, String> getDiskMigrationMap() {
        return diskMigrationMap;
    }

    public void setDiskMigrationMap(Map<String, String> diskMigrationMap) {
        this.diskMigrationMap = diskMigrationMap;
    }

    public long getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(long bandwidth) {
        this.bandwidth = bandwidth;
    }
}
