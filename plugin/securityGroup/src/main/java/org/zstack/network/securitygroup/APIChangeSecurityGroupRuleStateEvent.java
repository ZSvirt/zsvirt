package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;




@RestResponse(allTo = "inventory")
public class APIChangeSecurityGroupRuleStateEvent extends APIEvent {
    /**
     * @desc :ref:`SecurityGroupInventory`
     */
    private SecurityGroupInventory inventory;

    public APIChangeSecurityGroupRuleStateEvent() {
        super(null);
    }

    public APIChangeSecurityGroupRuleStateEvent(String apiId) {
        super(apiId);
    }

    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeSecurityGroupRuleStateEvent __example__() {
        APIChangeSecurityGroupRuleStateEvent event =  new APIChangeSecurityGroupRuleStateEvent();
        SecurityGroupInventory sec = new SecurityGroupInventory();
        sec.setUuid(uuid());
        sec.setName("web");
        sec.setDescription("for test");
        sec.setState("Enabled");
        sec.setCreateDate(new Timestamp(DocUtils.date));
        sec.setLastOpDate(new Timestamp(DocUtils.date));
        event.setInventory(sec);
        event.setSuccess(true);
        return event;
    }
}
