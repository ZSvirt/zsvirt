package org.zstack.autoscaling.template;

import org.zstack.autoscaling.group.AutoScalingGroupConstants;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Create by weiwang at 2018/8/16
 */
@RestResponse(allTo = "inventories")
public class APIQueryAutoScalingVmTemplateReply extends APIQueryReply {
    private List<AutoScalingVmTemplateInventory> inventories;

    public List<AutoScalingVmTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AutoScalingVmTemplateInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAutoScalingVmTemplateReply __example__() {
        APIQueryAutoScalingVmTemplateReply reply = new APIQueryAutoScalingVmTemplateReply();
        AutoScalingVmTemplateInventory inv = new AutoScalingVmTemplateInventory();

        String defaultL3Uuid = uuid();

        inv.setUuid(uuid());
        inv.setName("test-template");
        inv.setDescription("just for test");
        inv.setL3NetworkUuids(Arrays.asList(uuid(), defaultL3Uuid));
        inv.setType(AutoScalingGroupConstants.SCALING_RESOURCE_TYPE_VM_INSTANCE);
        inv.setRootDiskOfferingUuid(uuid());
        inv.setDataDiskOfferingUuids(Arrays.asList(uuid(), uuid()));
        inv.setVmInstanceZoneUuid(uuid());
        inv.setVmInstanceClusterUuid(uuid());
        inv.setHostUuid(uuid());
        inv.setPrimaryStorageUuidForRootVolume(uuid());
        inv.setDefaultL3NetworkUuid(uuid());
        inv.setDefaultL3NetworkUuid(defaultL3Uuid);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
