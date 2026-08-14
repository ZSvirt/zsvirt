package org.zstack.header.baremetal.chassis;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.NoDoc;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2019-01-20.
 *
 * Only for ZStack UI!
 */
@NoDoc
@RestRequest(
        path = "/baremetal/chassis/{chassisUuid}/actions",
        isAction = true,
        responseClass = APICleanUpBaremetalChassisBondingEvent.class,
        method = HttpMethod.PUT
)
public class APICleanUpBaremetalChassisBondingMsg extends APIMessage implements BaremetalChassisMessage {
    @APIParam(resourceType = BaremetalChassisVO.class)
    private String chassisUuid;

    public String getChassisUuid() {
        return chassisUuid;
    }

    public void setChassisUuid(String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }

    @Override
    public String getBaremetalChassisUuid() {
        return chassisUuid;
    }

    public static APICleanUpBaremetalChassisBondingMsg __example__() {
        APICleanUpBaremetalChassisBondingMsg msg = new APICleanUpBaremetalChassisBondingMsg();
        msg.setChassisUuid(uuid());
        return msg;
    }
}
