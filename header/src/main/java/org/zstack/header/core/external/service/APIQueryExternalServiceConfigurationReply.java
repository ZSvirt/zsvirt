package org.zstack.header.core.external.service;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 1:39 AM
 */
@RestResponse(allTo = "inventories")
public class APIQueryExternalServiceConfigurationReply extends APIQueryReply {
    private List<ExternalServiceConfigurationInventory> inventories;

    public List<ExternalServiceConfigurationInventory> getInventories() {return inventories;}

    public void setInventories(List<ExternalServiceConfigurationInventory> inventories) {this.inventories = inventories;}

    public static APIQueryExternalServiceConfigurationReply __example__() {
        APIQueryExternalServiceConfigurationReply reply = new APIQueryExternalServiceConfigurationReply();
        reply.setInventories(list(ExternalServiceConfigurationInventory.__example__()));
        return reply;
    }
}
