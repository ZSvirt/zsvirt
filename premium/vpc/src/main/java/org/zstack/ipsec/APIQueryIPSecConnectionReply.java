package org.zstack.ipsec;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Created by xing5 on 2016/11/11.
 */
@RestResponse(allTo = "inventories")
public class APIQueryIPSecConnectionReply extends APIQueryReply {
    private List<IPsecConnectionInventory> inventories;

    public List<IPsecConnectionInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<IPsecConnectionInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryIPSecConnectionReply __example__() {
        APIQueryIPSecConnectionReply reply = new APIQueryIPSecConnectionReply();
        IPsecConnectionInventory ipsec = new IPsecConnectionInventory();

        IPsecPeerCidrInventory cidr = new IPsecPeerCidrInventory();
        IPsecL3NetworkRefInventory l3Network = new IPsecL3NetworkRefInventory();
        cidr.setUuid(uuid());
        cidr.setCidr("192.168.100.0/24");
        cidr.setConnectionUuid(uuid());
        cidr.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        cidr.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));


        ipsec.setName("Test-IPSec");
        ipsec.setL3NetworkRefs(Arrays.asList(l3Network));
        ipsec.setPeerAddress("100.64.10.10");
        ipsec.setAuthKey("auth");
        ipsec.setVipUuid(uuid());
        ipsec.setPeerCidrs(Arrays.asList(cidr));

        reply.setInventories(Arrays.asList(ipsec));
        return reply;
    }

}
