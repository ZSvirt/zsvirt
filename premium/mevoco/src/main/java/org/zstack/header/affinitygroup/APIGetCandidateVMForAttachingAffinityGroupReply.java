package org.zstack.header.affinitygroup;

import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import static org.zstack.utils.CollectionDSL.list;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.header.affinitygroup
 * @date 2021/1/12 2:24 PM
 */
@RestResponse(fieldsTo = "all")
public class APIGetCandidateVMForAttachingAffinityGroupReply extends APIReply {
    List<VmInstanceInventory> inventories;

    public List<VmInstanceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmInstanceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetCandidateVMForAttachingAffinityGroupReply __example__() {
        APIGetCandidateVMForAttachingAffinityGroupReply reply = new APIGetCandidateVMForAttachingAffinityGroupReply();

        String defaultL3Uuid = uuid();
        String rootVolumeUuid = uuid();

        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setName("Test-VM");
        vm.setUuid(uuid());
        vm.setAllocatorStrategy(HostAllocatorConstant.LAST_HOST_PREFERRED_ALLOCATOR_STRATEGY_TYPE);
        vm.setClusterUuid(uuid());
        vm.setCpuNum(1);
        vm.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vm.setDefaultL3NetworkUuid(defaultL3Uuid);
        vm.setDescription("web server VM");
        vm.setHostUuid(uuid());
        vm.setHypervisorType("KVM");
        vm.setImageUuid(uuid());
        vm.setInstanceOfferingUuid(uuid());
        vm.setLastHostUuid(uuid());
        vm.setMemorySize(SizeUnit.GIGABYTE.toByte(8));
        vm.setPlatform("Linux");
        vm.setRootVolumeUuid(rootVolumeUuid);
        vm.setState(VmInstanceState.Stopped.toString());
        vm.setType(VmInstanceConstant.USER_VM_TYPE);
        vm.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vm.setZoneUuid(uuid());

        reply.setInventories(list(vm));

        return reply;
    }
}
