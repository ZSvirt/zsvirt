package org.zstack.header.baremetal.chassis;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2018-10-24.
 */
@RestRequest(
        path = "/baremetal/chassis/from-file/check",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICheckBaremetalChassisConfigFileReply.class
)
public class APICheckBaremetalChassisConfigFileMsg extends APISyncCallMessage {
    @APIParam(nonempty = true)
    private String baremetalChassisInfo;

    public String getBaremetalChassisInfo() {
        return baremetalChassisInfo;
    }

    public void setBaremetalChassisInfo(String baremetalChassisInfo) {
        this.baremetalChassisInfo = baremetalChassisInfo;
    }

    public static APICheckBaremetalChassisConfigFileMsg __example__() {
        APICheckBaremetalChassisConfigFileMsg msg = new APICheckBaremetalChassisConfigFileMsg();
        msg.setBaremetalChassisInfo("FILE CONTENT ENCODE BY BASE64");
        return msg;
    }
}
