package org.zstack.storage.migration;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.primary.PrimaryStorageLiveMigrateVmMsg;

import java.util.List;
import java.util.Map;

public interface KvmLiveMigrationWorkFlowTemplate {
    class ParamIn implements Cloneable {
        private PrimaryStorageLiveMigrateVmMsg msg;
        private VmInstanceInventory vmInstanceInventory;
        private KvmLiveMigrationWorkFlowTemplate template;
        private HostInventory dstHostInventory;
        private List<VolumeVO> volumesToMigrate;
        private List<VolumeInventory> volumesToKeep;
        private List<VolumeInventory> createdVolumes;
        private List<VolumeInventory> initializedVolumes;
        private Map<String, String> volumeMappingDict;
        private boolean migrateSucceeded = false;
        private PrimaryStorageMigrateVmContext jobContext;
        private long totalBandwidth;

        public PrimaryStorageMigrateVmContext getJobContext() {
            return jobContext;
        }

        public void setJobContext(PrimaryStorageMigrateVmContext jobContext) {
            this.jobContext = jobContext;
        }

        public boolean isMigrateSucceeded() {
            return migrateSucceeded;
        }

        public void setMigrateSucceeded(boolean migrateSucceeded) {
            this.migrateSucceeded = migrateSucceeded;
        }

        public VmInstanceInventory getVmInstanceInventory() {
            return vmInstanceInventory;
        }

        public void setVmInstanceInventory(VmInstanceInventory vmInstanceInventory) {
            this.vmInstanceInventory = vmInstanceInventory;
        }

        public List<VolumeInventory> getCreatedVolumes() {
            return createdVolumes;
        }

        public void setCreatedVolumes(List<VolumeInventory> createdVolumes) {
            this.createdVolumes = createdVolumes;
        }

        public List<VolumeInventory> getInitializedVolumes() {
            return initializedVolumes;
        }

        public void setInitializedVolumes(List<VolumeInventory> initializedVolumes) {
            this.initializedVolumes = initializedVolumes;
        }

        public Map<String, String> getVolumeMappingDict() {
            return volumeMappingDict;
        }

        public void setVolumeMappingDict(Map<String, String> volumeMappingDict) {
            this.volumeMappingDict = volumeMappingDict;
        }

        public List<VolumeInventory> getVolumesToKeep() {
            return volumesToKeep;
        }

        public void setVolumesToKeep(List<VolumeInventory> volumesToKeep) {
            this.volumesToKeep = volumesToKeep;
        }

        public KvmLiveMigrationWorkFlowTemplate getTemplate() {
            return template;
        }

        public void setTemplate(KvmLiveMigrationWorkFlowTemplate template) {
            this.template = template;
        }

        public List<VolumeVO> getVolumesToMigrate() {
            return volumesToMigrate;
        }

        public void setVolumesToMigrate(List<VolumeVO> volumesToMigrate) {
            this.volumesToMigrate = volumesToMigrate;
        }

        public HostInventory getDstHostInventory() {
            return dstHostInventory;
        }

        public void setDstHostInventory(HostInventory dstHostInventory) {
            this.dstHostInventory = dstHostInventory;
        }

        public long getTotalBandwidth() {
            return totalBandwidth;
        }

        public void setTotalBandwidth(long totalBandwidth) {
            this.totalBandwidth = totalBandwidth;
        }

        public PrimaryStorageLiveMigrateVmMsg getMsg() {
            return msg;
        }

        public void setMsg(PrimaryStorageLiveMigrateVmMsg msg) {
            this.msg = msg;
        }

        public ParamIn copy() {
            try {
                return (ParamIn) this.clone();
            } catch (CloneNotSupportedException e) {
                throw new CloudRuntimeException(e);
            }
        }
    }

    String getKvmStorageLiveMigrationType();

    List<Flow> getPrepareFlows(ParamIn paramIn);

    List<Flow> getPreMigrateFlows(ParamIn paramIn);

    List<Flow> getMigrateFlows(ParamIn paramIn);

    List<Flow> getAfterMigrateFlows(ParamIn paramIn);

    List<Flow> getCompleteFlows(ParamIn paramIn);

    ErrorCode preCheck(ParamIn paramIn);
}
