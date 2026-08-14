package org.zstack.header.baremetal.chassis;

import org.springframework.http.HttpMethod;
import org.zstack.header.longjob.APICreateLongJobMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2018-10-08.
 */
@RestRequest(
        path = "/baremetal/chassis/from-file",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIBatchCreateBaremetalChassisEvent.class
)
public class APIBatchCreateBaremetalChassisMsg extends APICreateLongJobMessage {
    @APIParam(nonempty = true)
    private String baremetalChassisInfo;

    public String getBaremetalChassisInfo() {
        return baremetalChassisInfo;
    }

    public void setBaremetalChassisInfo(String baremetalChassisInfo) {
        this.baremetalChassisInfo = baremetalChassisInfo;
    }

    public static APIBatchCreateBaremetalChassisMsg __example__() {
        APIBatchCreateBaremetalChassisMsg msg = new APIBatchCreateBaremetalChassisMsg();
        msg.setBaremetalChassisInfo("FILE CONTENT ENCODE BY BASE64");
        return msg;
    }
}
