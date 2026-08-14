package org.zstack.ipsec;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import java.sql.Timestamp;
import java.util.Arrays;

/**
 * Created by xing5 on 2016/11/3.
 */
@RestResponse(allTo = "inventory")
public class APICreateIPsecConnectionEvent extends APIEvent {

    private IPsecConnectionInventory inventory;

    public APICreateIPsecConnectionEvent() {
    }

    public APICreateIPsecConnectionEvent(String apiId) {
        super(apiId);
    }

    public IPsecConnectionInventory getInventory() {
        return inventory;
    }

    public void setInventory(IPsecConnectionInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateIPsecConnectionEvent __example__() {
        APICreateIPsecConnectionEvent event = new APICreateIPsecConnectionEvent();
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
        ipsec.setStatus(IPSecStatus.Connecting.toString());
        event.setInventory(ipsec);
        return event;
    }
}
