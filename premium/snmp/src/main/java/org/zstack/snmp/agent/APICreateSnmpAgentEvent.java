package org.zstack.snmp.agent;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;

/**
 *
 * @Author : jingwang
 * @create 2023/8/28 17:47
 */
@RestResponse(allTo = "inventory")
public class APICreateSnmpAgentEvent extends APIEvent {
    private SnmpAgentInventory inventory;

    public APICreateSnmpAgentEvent() {
    }

    public static APICreateSnmpAgentEvent __example__() {
        APICreateSnmpAgentEvent event = new APICreateSnmpAgentEvent();
        SnmpAgentInventory inventory = new SnmpAgentInventory();
        inventory.setUuid(uuid());
        inventory.setVersion(SnmpAgentVersion.v3.name());
        inventory.setUserName("zstack");
        inventory.setAuthAlgorithm(SnmpAgentAuthAlgorithm.SHA512.name());
        inventory.setAuthPassword("auth_password");
        inventory.setPrivacyAlgorithm(SnmpAgentPrivacyAlgorithm.DES.name);
        inventory.setPrivacyPassword("priv_password");
        inventory.setPort(161);
        inventory.setStatus(SnmpAgentStatus.Enable.name());
        inventory.setSecurityLevel(SecurityLevel.authPriv.name());
        event.setInventory(inventory);
        return event;
    }

    public APICreateSnmpAgentEvent(String apiId) {
        super(apiId);
    }

    public SnmpAgentInventory getInventory() {
        return inventory;
    }

    public void setInventory(SnmpAgentInventory inventory) {
        this.inventory = inventory;
    }
}
