package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @ Author : yh.w
 * @ Date   : Created in 21:27 2019/9/18
 */
@RestRequest(
        path = "/vm-priority-config/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdatePriorityConfigEvent.class
)
public class APIUpdatePriorityConfigMsg extends APIMessage {

    @APIParam(resourceType = VmPriorityConfigVO.class)
    private String uuid;

    @APIParam(required = false, numberRange = {2, 262144})
    private Integer cpuShares;

    @APIParam(required = false, numberRange = {-1000, 1000})
    private Integer oomScoreAdj;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getCpuShares() {
        return cpuShares;
    }

    public void setCpuShares(Integer cpuShares) {
        this.cpuShares = cpuShares;
    }

    public Integer getOomScoreAdj() {
        return oomScoreAdj;
    }

    public void setOomScoreAdj(Integer oomScoreAdj) {
        this.oomScoreAdj = oomScoreAdj;
    }

    public static APIUpdatePriorityConfigMsg __example__() {
        APIUpdatePriorityConfigMsg msg = new APIUpdatePriorityConfigMsg();
        msg.uuid = uuid();
        msg.cpuShares = 2;
        msg.oomScoreAdj = 100;
        return msg;
    }
}
