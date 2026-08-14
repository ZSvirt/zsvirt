package org.zstack.ipsec;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by shixin on 2017/12/07.
 */
@RestResponse(allTo = "inventory")
public class APIRemoveRemoteCidrsFromIPsecConnectionEvent extends APIEvent {

    private IPsecConnectionInventory inventory;

    public APIRemoveRemoteCidrsFromIPsecConnectionEvent() {
    }

    public APIRemoveRemoteCidrsFromIPsecConnectionEvent(String apiId) {
        super(apiId);
    }

    public IPsecConnectionInventory getInventory() {
        return inventory;
    }

    public void setInventory(IPsecConnectionInventory inventory) {
        this.inventory = inventory;
    }

    public static APIRemoveRemoteCidrsFromIPsecConnectionEvent __example__() {
        APIRemoveRemoteCidrsFromIPsecConnectionEvent event = new APIRemoveRemoteCidrsFromIPsecConnectionEvent();
        IPsecConnectionInventory ipsec = new IPsecConnectionInventory();
        IPsecPeerCidrInventory cidr = new IPsecPeerCidrInventory();
        IPsecL3NetworkRefInventory l3Network = new IPsecL3NetworkRefInventory();
        ipsec.setUuid(uuid());
        ipsec.setName("Test-IPSec");
        ipsec.setPeerAddress("100.64.10.10");
        ipsec.setAuthKey("auth");
        ipsec.setVipUuid(uuid());

        l3Network.setUuid(uuid());
        l3Network.setConnectionUuid(ipsec.getUuid());
        l3Network.setL3NetworkUuid(uuid());
        l3Network.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        l3Network.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ipsec.setL3NetworkRefs(Collections.singletonList(l3Network));

        cidr.setUuid(uuid());
        cidr.setCidr("192.168.100.0/24");
        cidr.setConnectionUuid(ipsec.getUuid());
        cidr.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        cidr.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ipsec.setPeerCidrs(Collections.singletonList(cidr));

        event.setInventory(ipsec);
        return event;
    }
}
