package org.zstack.iam1.entity.ensemble;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vo.ResourceInventory;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.CollectionUtils;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2024/08/06
 */
@PythonClassInventory
public class ResourceEnsembleInventory {
    private String masterUuid;
    private String masterResourceName;
    private String masterResourceType;
    private List<ResourceInventory> members;

    public static ResourceEnsembleInventory valueOf(ResourceVO masterResource, List<ResourceVO> list) {
        ResourceEnsembleInventory inv = new ResourceEnsembleInventory();
        inv.setMasterUuid(masterResource.getUuid());
        inv.setMasterResourceName(masterResource.getResourceName());
        inv.setMasterResourceType(masterResource.getResourceType());
        inv.setMembers(CollectionUtils.transform(list, ResourceInventory::valueOf));
        return inv;
    }

    public String getMasterUuid() {
        return masterUuid;
    }

    public void setMasterUuid(String masterUuid) {
        this.masterUuid = masterUuid;
    }

    public String getMasterResourceName() {
        return masterResourceName;
    }

    public void setMasterResourceName(String masterResourceName) {
        this.masterResourceName = masterResourceName;
    }

    public String getMasterResourceType() {
        return masterResourceType;
    }

    public void setMasterResourceType(String masterResourceType) {
        this.masterResourceType = masterResourceType;
    }

    public List<ResourceInventory> getMembers() {
        return members;
    }

    public void setMembers(List<ResourceInventory> members) {
        this.members = members;
    }

    public static ResourceEnsembleInventory __example__() {
        ResourceEnsembleInventory inventory = new ResourceEnsembleInventory();
        inventory.setMasterUuid("14c61568f49a45759c9a75c8fea4f854");
        inventory.setMasterResourceName("VM1");
        inventory.setMasterResourceType(VmInstanceVO.class.getSimpleName());

        ResourceInventory r1 = new ResourceInventory();
        r1.setUuid("fbf9fc27210b42748e87306a49eaac38");
        r1.setResourceName("root-volume-VM1");
        r1.setResourceType(VolumeVO.class.getSimpleName());

        ResourceInventory r2 = new ResourceInventory();
        r1.setUuid("0e536c9206f2494fbdc516c47fbef0cb");
        r1.setResourceName("vmnic-VM1");
        r1.setResourceType(VmNicVO.class.getSimpleName());

        ResourceInventory r3 = new ResourceInventory();
        r1.setUuid("c1abb7bd0de8489bb183d829c3ccde0e");
        r1.setResourceName("vmcdrom-VM1");
        r1.setResourceType(VmCdRomVO.class.getSimpleName());

        inventory.setMembers(list(r1, r2, r3));
        return inventory;
    }
}
