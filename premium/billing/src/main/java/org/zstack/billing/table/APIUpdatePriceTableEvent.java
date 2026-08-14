package org.zstack.billing.table;

import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

import static java.util.Arrays.asList;

/**
 * Created by lining on 2019/9/10.
 */

@RestResponse(allTo = "inventory")
public class APIUpdatePriceTableEvent extends APIEvent {
    private PriceTableInventory inventory;

    public APIUpdatePriceTableEvent() {
    }

    public APIUpdatePriceTableEvent(String apiId) {
        super(apiId);
    }

    public PriceTableInventory getInventory() {
        return inventory;
    }

    public void setInventory(PriceTableInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdatePriceTableEvent __example__() {
        APIUpdatePriceTableEvent event = new APIUpdatePriceTableEvent();
        PriceTableInventory inventory = new PriceTableInventory();
        inventory.setUuid(uuid());
        inventory.setName("price table");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inventory);
        return event;
    }

}
