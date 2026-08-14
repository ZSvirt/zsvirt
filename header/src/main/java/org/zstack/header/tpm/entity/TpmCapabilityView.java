package org.zstack.header.tpm.entity;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.vm.additions.VmHostFileInventory;

import java.sql.Timestamp;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@PythonClass
public class TpmCapabilityView {
    // fields in TpmInventory
    private String uuid;
    private String name;
    private String vmInstanceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    /**
     * collect VmHostFileInventory(VmHostFileVO) type=NvRam or type=TpmState
     */
    private List<VmHostFileInventory> fileRefs;

    // related table fields
    // TODO  keyProviderUuid / keyProviderType / keyProviderName / keyProviderKeyVersion

    // status fields : from system tags
    private String edkVersion;
    private String swtpmVersion;

    // config fields : from global / resource config
    private boolean resetTpmAfterVmCloneConfig;

    public void setTpmInventory(TpmInventory inventory) {
        setUuid(inventory.getUuid());
        setName(inventory.getName());
        setVmInstanceUuid(inventory.getVmInstanceUuid());
        setCreateDate(inventory.getCreateDate());
        setLastOpDate(inventory.getLastOpDate());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public List<VmHostFileInventory> getFileRefs() {
        return fileRefs;
    }

    public void setFileRefs(List<VmHostFileInventory> fileRefs) {
        this.fileRefs = fileRefs;
    }

    public String getEdkVersion() {
        return edkVersion;
    }

    public void setEdkVersion(String edkVersion) {
        this.edkVersion = edkVersion;
    }

    public String getSwtpmVersion() {
        return swtpmVersion;
    }

    public void setSwtpmVersion(String swtpmVersion) {
        this.swtpmVersion = swtpmVersion;
    }

    public boolean isResetTpmAfterVmCloneConfig() {
        return resetTpmAfterVmCloneConfig;
    }

    public void setResetTpmAfterVmCloneConfig(boolean resetTpmAfterVmCloneConfig) {
        this.resetTpmAfterVmCloneConfig = resetTpmAfterVmCloneConfig;
    }

    public static TpmCapabilityView __example__() {
        TpmCapabilityView view = new TpmCapabilityView();
        view.setTpmInventory(TpmInventory.__example__());
        view.setFileRefs(list(VmHostFileInventory.__example__()));

        view.setEdkVersion("edk2-ovmf-20220126gitbb1bba3d77-3.el8.noarch");
        view.setSwtpmVersion("0.8.2");

        view.setResetTpmAfterVmCloneConfig(true);
        return view;
    }
}
