package org.zstack.compute.vm;

import org.zstack.tag.SystemTagCreator;

import java.util.Collections;

public class VmConfigSyncHelper {
    public void setVmSyncPorts(String vmUuid) {
        SystemTagCreator creator = VmSystemTags.SYNC_PORTS.newSystemTagCreator(vmUuid);
        creator.recreate = true;
        creator.setTagByTokens(Collections.singletonMap(VmSystemTags.SYNC_PORTS_TOKEN, vmUuid));
        creator.create();
    }

    // If the VM has this system tag,
    // when both the QGA status and the tool status are available,
    // or when synchronizing the internal IP of the virtual machine,
    // it will trigger the operation to synchronize the database IP to the VM.
    public boolean vmNeedSyncPorts(String vmUuid) {
        return VmSystemTags.SYNC_PORTS.hasTag(vmUuid);
    }

    // sync hostname and DNS at the same time
    public void afterVmSyncPorts(String vmUuid) {
        VmSystemTags.SYNC_PORTS.delete(vmUuid);
        afterVmSyncHostname(vmUuid);
        afterVmSyncDns(vmUuid);
    }

    public void setVmSyncHostname(String vmUuid) {
        SystemTagCreator creator = VmSystemTags.SYNC_HOSTNAME.newSystemTagCreator(vmUuid);
        creator.recreate = true;
        creator.setTagByTokens(Collections.singletonMap(VmSystemTags.SYNC_HOSTNAME_TOKEN, vmUuid));
        creator.create();
    }

    public boolean vmNeedSyncHostname(String vmUuid) {
        return VmSystemTags.SYNC_HOSTNAME.hasTag(vmUuid);
    }

    public void afterVmSyncHostname(String vmUuid) {
        VmSystemTags.SYNC_HOSTNAME.delete(vmUuid);
    }

    // only used for non-Windows vm to sync DNS when qga ready,
    // it will be set when qga ready,
    // for non-Windows to avoid the inability to sync DNS when there is no ip inside or outside,
    // for Windows, it will be converted to sync ports tag when first syncing nic info from vm
    public void setVmSyncDns(String vmUuid) {
        SystemTagCreator creator = VmSystemTags.SYNC_DNS.newSystemTagCreator(vmUuid);
        creator.recreate = true;
        creator.create();
    }

    public boolean vmNeedSyncDns(String vmUuid) {
        return VmSystemTags.SYNC_DNS.hasTag(vmUuid);
    }

    public void afterVmSyncDns(String vmUuid) {
        VmSystemTags.SYNC_DNS.delete(vmUuid);
    }
}
