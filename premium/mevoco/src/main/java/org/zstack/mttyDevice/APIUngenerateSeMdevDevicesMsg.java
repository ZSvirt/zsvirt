package org.zstack.mttyDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(
        path = "/mtty-devices/{mttyDeviceUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUngenerateSeMdevDevicesEvent.class,
        isAction = true
)
public class APIUngenerateSeMdevDevicesMsg extends APIMessage {
    @APIParam(resourceType = MttyDeviceVO.class)
    private String mttyDeviceUuid;

    @APINoSee
    private List<String> mdevUuids;

    public String getMttyDeviceUuid() {
        return mttyDeviceUuid;
    }

    public void setMttyDeviceUuid(String mttyDeviceUuid) {
        this.mttyDeviceUuid = mttyDeviceUuid;
    }

    public List<String> getMdevUuids() {
        return mdevUuids;
    }

    public void setMdevUuids(List<String> mdevUuids) {
        this.mdevUuids = mdevUuids;
    }
    
    public static APIUngenerateSeMdevDevicesMsg __example__() {
        APIUngenerateSeMdevDevicesMsg msg = new APIUngenerateSeMdevDevicesMsg();
        msg.setMttyDeviceUuid(uuid());
        return msg;
    }
}
