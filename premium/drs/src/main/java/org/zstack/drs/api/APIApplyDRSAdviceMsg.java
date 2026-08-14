package org.zstack.drs.api;

import org.springframework.http.HttpMethod;
import org.zstack.drs.DRSMessage;
import org.zstack.drs.entity.DRSAdviceVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Create by lining at 2019/12/12
 */
@RestRequest(
        path = "/clusters/drs/advice/{adviceUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIApplyDRSAdviceEvent.class,
        isAction = true
)
public class APIApplyDRSAdviceMsg extends APIMessage implements DRSMessage {
    @APIParam(resourceType = DRSAdviceVO.class)
    private String adviceUuid;

    @APINoSee
    private String drsUuid;

    @Override
    public String getDRSUuid() {
        return drsUuid;
    }

    public String getAdviceUuid() {
        return adviceUuid;
    }

    public void setAdviceUuid(String adviceUuid) {
        this.adviceUuid = adviceUuid;
    }

    public String getDrsUuid() {
        return drsUuid;
    }

    public void setDrsUuid(String drsUuid) {
        this.drsUuid = drsUuid;
    }

    public static APIApplyDRSAdviceMsg __example__() {
        APIApplyDRSAdviceMsg msg = new APIApplyDRSAdviceMsg();
        msg.setAdviceUuid(uuid(DRSAdviceVO.class));
        return msg;
    }
}
