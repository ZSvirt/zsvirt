package org.zstack.header.vm;

import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.kvm.hypervisor.datatype.HypervisorVersionState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 23/02/21
 */
@RestResponse(fieldsTo = {"inventories"})
public class APIGetVirtualizerInfoReply extends APIReply {
    private List<VirtualizerInfoInventory> inventories;

    public List<VirtualizerInfoInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VirtualizerInfoInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVirtualizerInfoReply __example__() {
        APIGetVirtualizerInfoReply reply = new APIGetVirtualizerInfoReply();

        List<VirtualizerInfoInventory> list = new ArrayList<>();

        // host
        VirtualizerInfoInventory host = new VirtualizerInfoInventory();
        host.setUuid(uuid());
        host.setResourceType(HostVO.class.getSimpleName());
        VirtualizerInfo info = new VirtualizerInfo();
        info.setHypervisor("qemu-kvm");
        info.setCurrentVersion("4.2.0-632.g6a6222b.el7");
        info.setExpectVersion("4.2.0-632.g6a6222b.el7");
        info.setMatchState(HypervisorVersionState.Matched);
        host.setInfoList(Collections.singletonList(info));
        list.add(host);

        // vm1
        VirtualizerInfoInventory vm1 = new VirtualizerInfoInventory();
        vm1.setUuid(uuid());
        vm1.setResourceType(VmInstanceVO.class.getSimpleName());
        VirtualizerInfo info1 = new VirtualizerInfo();
        info1.setHypervisor("qemu-kvm");
        info1.setCurrentVersion("4.2.0-632.g6a6222b.el7");
        info1.setExpectVersion("4.2.0-632.g6a6222b.el7");
        info1.setMatchState(HypervisorVersionState.Matched);
        vm1.setInfoList(Collections.singletonList(info1));
        list.add(vm1);

        // vm2
        VirtualizerInfoInventory vm2 = new VirtualizerInfoInventory();
        vm2.setUuid(uuid());
        vm2.setResourceType(VmInstanceVO.class.getSimpleName());
        VirtualizerInfo info2 = new VirtualizerInfo();
        info2.setHypervisor("qemu-kvm");
        info2.setCurrentVersion("4.2.0-632.g6a6222b.el7");
        info2.setExpectVersion("4.2.0-627.g6a6222b.el7");
        info2.setMatchState(HypervisorVersionState.Unmatched);
        vm2.setInfoList(Collections.singletonList(info1));
        list.add(vm2);

        reply.setInventories(list);
        return reply;
    }
}
