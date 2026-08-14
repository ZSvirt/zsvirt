package org.zstack.mttyDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @author yu.sun
 * @date 2022/11/22 14:12
 **/
@RestRequest(
        path = "/mtty-devices/{mttyDeviceUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIGenerateSeMdevDevicesEvent.class,
        isAction = true
)
public class APIGenerateSeMdevDevicesMsg extends APIMessage {
    @APIParam(resourceType = MttyDeviceVO.class)
    private String mttyDeviceUuid;

    @APIParam
    private Integer virtPartNum;

    public String getMttyDeviceUuid() {
        return mttyDeviceUuid;
    }

    public void setMttyDeviceUuid(String mttyDeviceUuid) {
        this.mttyDeviceUuid = mttyDeviceUuid;
    }

    public Integer getVirtPartNum() {
        return virtPartNum;
    }

    public void setVirtPartNum(Integer virtPartNum) {
        this.virtPartNum = virtPartNum;
    }

    public static APIGenerateSeMdevDevicesMsg __example__() {
        APIGenerateSeMdevDevicesMsg msg = new APIGenerateSeMdevDevicesMsg();
        msg.setMttyDeviceUuid(uuid());
        msg.setVirtPartNum(4);
        return msg;
    }
}