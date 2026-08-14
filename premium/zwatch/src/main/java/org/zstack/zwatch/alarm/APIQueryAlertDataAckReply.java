package org.zstack.zwatch.alarm;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryAlertDataAckReply extends APIQueryReply {
    private List<AlertDataAckInventory> inventories;

    public static APIQueryAlertDataAckReply __example__() {
        APIQueryAlertDataAckReply ret = new APIQueryAlertDataAckReply();
        APIAckAlarmDataMsg msg = APIAckAlarmDataMsg.__example__();
        AlertDataAckInventory inventory = new AlertDataAckInventory();
        inventory.setAlertDataUuid(msg.getAlertDataUuid());
        inventory.setAlertType(msg.getDataType());
        inventory.setAckPeriod(msg.getAckPeriodSec().longValue());
        inventory.setResourceUuid(msg.getResourceUuid());
        inventory.setAckDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setResumeAlert(false);
        inventory.setOperatorAccountUuid(uuid());
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<AlertDataAckInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AlertDataAckInventory> inventories) {
        this.inventories = inventories;
    }
}
