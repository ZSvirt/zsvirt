package org.zstack.vpc;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.ipsec.IPsecConnectionInventory;
import org.zstack.ipsec.IPsecL3NetworkRefInventory;
import org.zstack.ipsec.IPsecPeerCidrInventory;
import java.util.Arrays;
import java.util.List;

/**
 * Created by shixin.ruan 2021/03/23
 */
@RestResponse(allTo = "inventories")
public class APIGetVpcAttachedIpsecReply extends APIReply {
    private List<IPsecConnectionInventory> inventories;

    public List<IPsecConnectionInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<IPsecConnectionInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVpcAttachedIpsecReply __example__() {
        APIGetVpcAttachedIpsecReply reply = new APIGetVpcAttachedIpsecReply();

        IPsecConnectionInventory ipsec = new IPsecConnectionInventory();
        IPsecPeerCidrInventory cidr = new IPsecPeerCidrInventory();
        IPsecL3NetworkRefInventory l3Network = new IPsecL3NetworkRefInventory();
        cidr.setUuid(uuid());
        cidr.setCidr("192.168.100.0/24");
        cidr.setConnectionUuid(uuid());
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
