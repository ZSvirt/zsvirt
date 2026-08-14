package org.zstack.snmp.agent;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *
 * @Author : jingwang
 * @create 2023/7/14 4:30 PM
 */
@RestResponse(allTo = "inventory")
public class APIUpdateSnmpAgentEvent extends APIEvent {
    SnmpAgentInventory inventory;

    public APIUpdateSnmpAgentEvent() {
    }

    public APIUpdateSnmpAgentEvent(String apiId) {
        super(apiId);
    }

    public SnmpAgentInventory getInventory() {
        return inventory;
    }

    public void setInventory(SnmpAgentInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateSnmpAgentEvent __example__() {
        APIUpdateSnmpAgentEvent event = new APIUpdateSnmpAgentEvent();
        SnmpAgentInventory inventory = new SnmpAgentInventory();
        inventory.setUuid(uuid(SnmpAgentVO.class));
        inventory.setAuthAlgorithm(SnmpAgentAuthAlgorithm.SHA.name());
        inventory.setAuthPassword("authPassword");
        inventory.setPrivacyAlgorithm(SnmpAgentPrivacyAlgorithm.AES128.name);
        inventory.setPrivacyPassword("privPassword");
        inventory.setPort(162);
        inventory.setVersion(SnmpAgentVersion.v3.name());
        inventory.setUserName("zstack");
        inventory.setReadCommunity(null);
        event.setInventory(inventory);
        return event;

    }
}
