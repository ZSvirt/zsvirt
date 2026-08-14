package org.zstack.storage.device.iscsi;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryIscsiLunReply extends APIQueryReply {
    private List<IscsiLunInventory> inventories;

    public List<IscsiLunInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<IscsiLunInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryIscsiLunReply __example__() {
        APIQueryIscsiLunReply reply = new APIQueryIscsiLunReply();

        IscsiLunInventory iscsiLunInventory = new IscsiLunInventory();
        iscsiLunInventory.setUuid(uuid());
        iscsiLunInventory.setWwn("0x6b083fe000daf018");
        iscsiLunInventory.setHctl("6:0:1:1");
        iscsiLunInventory.setModel("MD32xx");
        iscsiLunInventory.setVendor("DELL");
        iscsiLunInventory.setType("mpath");
        iscsiLunInventory.setSerial("6b083fe000daf018000015505abbe00a");
        iscsiLunInventory.setSize(30003188203520l);
        iscsiLunInventory.setWwid("36b083fe000daf018000015505abbe00a");
        iscsiLunInventory.setPath("ip-0.0.0.201:3260-iscsi-iqn.2018-01.io.zstack:tsn.00001-lun-0");
        iscsiLunInventory.setMultipathDeviceUuid("36b083fe000daf018000015505abbe00a");

        IscsiLunInventory iscsiLunInventory2 = new IscsiLunInventory();
        iscsiLunInventory2.setUuid(uuid());
        iscsiLunInventory2.setWwn("0x6b083fe000daf018");
        iscsiLunInventory2.setHctl("6:0:3:1");
        iscsiLunInventory2.setModel("MD32xx");
        iscsiLunInventory2.setVendor("DELL");
        iscsiLunInventory2.setType("mpath");
        iscsiLunInventory2.setSerial("6b083fe000daf018000015505abbe00a");
        iscsiLunInventory2.setSize(30003188203520l);
        iscsiLunInventory2.setWwid("36b083fe000daf018000015505abbe00a");
        iscsiLunInventory2.setPath("ip-0.0.0.201:3260-iscsi-iqn.2018-01.io.zstack:tsn.00002-lun-0");
        iscsiLunInventory2.setMultipathDeviceUuid("36b083fe000daf018000015505abbe00a");

        reply.setInventories(asList(iscsiLunInventory, iscsiLunInventory2));
        reply.setSuccess(true);
        return reply;
    }
}
