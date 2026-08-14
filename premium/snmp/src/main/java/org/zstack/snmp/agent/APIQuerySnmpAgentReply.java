package org.zstack.snmp.agent;

import org.zstack.core.Platform;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;

import java.util.List;

import static java.util.Arrays.asList;

/**
 *
 * @Author : jingwang
 * @create 2023/8/1 10:31
 */
@RestResponse(allTo = "inventories")
public class APIQuerySnmpAgentReply extends APIQueryReply {
    private List<SnmpAgentInventory> inventories;

    public List<SnmpAgentInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SnmpAgentInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySnmpAgentReply __example__() {
        APIQuerySnmpAgentReply ret = new APIQuerySnmpAgentReply();
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
        ret.inventories = asList(inventory);
        return ret;
    }
}
