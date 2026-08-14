package org.zstack.zwatch.thirdparty.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.thirdparty.entity.SNSEndpointThirdpartyAlertHistoryInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQuerySNSEndpointThirdpartyAlertHistoryReply extends APIQueryReply {
    private List<SNSEndpointThirdpartyAlertHistoryInventory> inventories;

    public static APIQuerySNSEndpointThirdpartyAlertHistoryReply __example__() {
        APIQuerySNSEndpointThirdpartyAlertHistoryReply ret = new APIQuerySNSEndpointThirdpartyAlertHistoryReply();
        SNSEndpointThirdpartyAlertHistoryInventory inventory = SNSEndpointThirdpartyAlertHistoryInventory.__example__();
        inventory.setAlertUuid(uuid());
        inventory.setEndpointUuid(uuid());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<SNSEndpointThirdpartyAlertHistoryInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSEndpointThirdpartyAlertHistoryInventory> inventories) {
        this.inventories = inventories;
    }
}
