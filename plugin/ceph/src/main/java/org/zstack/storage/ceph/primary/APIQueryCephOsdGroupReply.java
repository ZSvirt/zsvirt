package org.zstack.storage.ceph.primary;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.storage.ceph.DataSecurityPolicy;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryCephOsdGroupReply extends APIQueryReply {
    private List<CephOsdGroupInventory> inventories;

    public List<CephOsdGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<CephOsdGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryCephOsdGroupReply __example__() {
        APIQueryCephOsdGroupReply reply = new APIQueryCephOsdGroupReply();
        CephOsdGroupInventory cephOsdGroup = new CephOsdGroupInventory();
        cephOsdGroup.setOsds("osd.1");
        cephOsdGroup.setAvailablePhysicalCapacity(1024);
        cephOsdGroup.setTotalPhysicalCapacity(1024);
        cephOsdGroup.setAvailableCapacity(0);
        cephOsdGroup.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        cephOsdGroup.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(asList(cephOsdGroup));
        reply.setSuccess(true);
        return reply;
    }

}
