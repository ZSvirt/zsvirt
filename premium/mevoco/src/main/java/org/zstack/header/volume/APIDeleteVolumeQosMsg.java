package org.zstack.header.volume;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import static org.zstack.header.volume.MevocoVolumeConstants.*;

/**
 * Created by mingjian.deng on 17/1/11.
 */
@RestRequest(
        path = "/volumes/{uuid}/qos",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVolumeQosEvent.class
)
public class APIDeleteVolumeQosMsg extends APIMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String uuid;

    @APIParam(validValues={VOLUME_QOS_MODE_TOTAL, VOLUME_QOS_MODE_READ, VOLUME_QOS_MODE_WRITE, VOLUME_QOS_MODE_ALL,
            VOLUME_QOS_MODE_OVERWRITE}, required=false)
    private String mode = VOLUME_QOS_MODE_TOTAL;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = StringUtils.isNotEmpty(mode) ? mode : VOLUME_QOS_MODE_TOTAL;
    }

    public static APIDeleteVolumeQosMsg __example__() {
        APIDeleteVolumeQosMsg msg = new APIDeleteVolumeQosMsg();
        msg.setUuid(uuid());

        return msg;
    }

}
