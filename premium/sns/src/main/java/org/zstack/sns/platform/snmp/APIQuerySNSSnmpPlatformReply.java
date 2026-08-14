package org.zstack.sns.platform.snmp;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * @Author : jingwang
 * @create 2023/8/25 11:01
 */
@RestResponse(allTo = "inventories")
public class APIQuerySNSSnmpPlatformReply extends APIQueryReply {
    private List<SNSSnmpPlatformInventory> inventories;

    public List<SNSSnmpPlatformInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSSnmpPlatformInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySNSSnmpPlatformReply __example__() {
        APIQuerySNSSnmpPlatformReply r = new APIQuerySNSSnmpPlatformReply();
        final SNSSnmpPlatformInventory inventory = new SNSSnmpPlatformInventory();
        inventory.setUuid(uuid());
        inventory.setName("snmp platform");
        inventory.setSnmpAddress("127.0.0.1");
        inventory.setSnmpPort(161);
        r.setInventories(Arrays.asList(inventory));
        return r;
    }
}
