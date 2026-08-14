package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.alarm.AlarmVO;

@RestRequest(
        path = "/zwatch/metrics/httpreceivers",
        method = HttpMethod.POST,
        responseClass = APICreateMetricDataHttpReceiverEvent.class,
        parameterName = "params"
)
public class APICreateMetricDataHttpReceiverMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 255)
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(required = false)
    private boolean defaultEnable;

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateMetricDataHttpReceiverEvent)rsp).getInventory().getUuid() : "", AlarmVO.class);
    }

    public static APICreateMetricDataHttpReceiverMsg __example__() {
        APICreateMetricDataHttpReceiverMsg msg = new APICreateMetricDataHttpReceiverMsg();
        msg.name = "CloudMonitor";
        msg.defaultEnable = true;
        msg.url = "http://127.0.0.1/xxx";
        return msg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefaultEnable() {
        return defaultEnable;
    }

    public void setDefaultEnable(boolean defaultEnable) {
        this.defaultEnable = defaultEnable;
    }
}
