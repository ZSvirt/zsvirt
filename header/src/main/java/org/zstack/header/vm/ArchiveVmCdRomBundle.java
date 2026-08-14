package org.zstack.header.vm;

import org.zstack.header.vm.cdrom.DeleteVmCdRomMsg;
import org.zstack.header.vm.cdrom.VmCdRomInventory;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by LiangHanYu on 2022/9/26 17:44
 */
public class ArchiveVmCdRomBundle extends ArchiveBundle {
    VmCdRomInventory cdRomInventory;

    public ArchiveVmCdRomBundle() {
    }

    public ArchiveVmCdRomBundle(VmCdRomInventory cdRomInventory) {
        this.cdRomInventory = cdRomInventory;
    }

    public VmCdRomInventory getCdRomInventory() {
        return cdRomInventory;
    }

    public void setCdRomInventory(VmCdRomInventory cdRomInventory) {
        this.cdRomInventory = cdRomInventory;
    }

    public static CreateVmCdRomMsg toCreateVmCdRomMsg(String vmInstanceUuid, String metadata) {
        VmCdRomInventory cdRomInventory = JSONObjectUtil.toObject(metadata, ArchiveVmCdRomBundle.class).getCdRomInventory();
        CreateVmCdRomMsg cmsg = new CreateVmCdRomMsg();
        cmsg.setVmInstanceUuid(vmInstanceUuid);
        cmsg.setName(cdRomInventory.getName());
        cmsg.setResourceUuid(cdRomInventory.getUuid());
        cmsg.setDescription(cdRomInventory.getDescription());
        return cmsg;
    }


    public static DeleteVmCdRomMsg toDeleteVmCdRomMsg(String vmInstanceUuid, String cdRomUuid) {
        DeleteVmCdRomMsg dmsg = new DeleteVmCdRomMsg();
        dmsg.setVmInstanceUuid(vmInstanceUuid);
        dmsg.setCdRomUuid(cdRomUuid);
        return dmsg;
    }
}
