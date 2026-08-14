package org.zstack.snmp.agent;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;

/**
 *
 * @Author : jingwang
 * @create 2023/7/13 2:21 PM
 */
@RestResponse(allTo = "inventory")
public class APIStopSnmpAgentEvent extends APIEvent {
    private SnmpAgentInventory inventory;

    public APIStopSnmpAgentEvent() {
        super(null);
    }

    public APIStopSnmpAgentEvent(String apiId) {
        super(apiId);
    }

    public static APIStopSnmpAgentEvent __example__() {
        APIStopSnmpAgentEvent event = new APIStopSnmpAgentEvent();
        SnmpAgentInventory inventory = new SnmpAgentInventory();
        inventory.setUuid(uuid(SnmpAgentVO.class));
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

    public SnmpAgentInventory getInventory() {
        return inventory;
    }

    public void setInventory(SnmpAgentInventory inventory) {
        this.inventory = inventory;
    }
}
