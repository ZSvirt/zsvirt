package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.metricpusher.MetricDataHttpReceiverVO;
import org.zstack.zwatch.metricpusher.MetricTemplateVO;

@RestRequest(
        path = "/zwatch/metrics/httpreceivers/templates",
        method = HttpMethod.POST,
        responseClass = APICreateMetricTemplateEvent.class,
        parameterName = "params"
)
public class APICreateMetricTemplateMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 32, resourceType = MetricDataHttpReceiverVO.class)
    private String receiverUuid;

    @APIParam(maxLength = 4096)
    private String template;

    @APIParam(maxLength = 64)
    private String namespace;

    @APIParam(maxLength = 128)
    private String metricName;

    @APIParam(maxLength = 4096, required = false)
    private String labelsJsonStr;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateMetricTemplateEvent)rsp).getInventory().getUuid() : "", MetricTemplateVO.class);
    }

    public static APICreateMetricTemplateMsg __example__() {
        APICreateMetricTemplateMsg msg = new APICreateMetricTemplateMsg();
        msg.labelsJsonStr = "['VMUuid=95e5885772de4f2bb3e05eba98a43cd0']";
        msg.metricName = "DiskReadOps";
        msg.namespace = "ZStack/VM";
        msg.receiverUuid = uuid();
        msg.template = "{" +
                "  \"metricName\":\"${METRIC_NAME}\",\n" +
                "  \"regionId\":\"cn-shanghai\",\n" +
                "  \"instanceId\":\"${RESOURCE_UUID}\",\n" +
                "  \"resource\":\"vm/${RESOURCE_NAME}\",\n" +
                "  \"dimensions\":{\n" +
                "      \"instanceId\":\"${METRIC_LABLES.get('VMUuid')}\",\n" +
                "      \"device\":\"${METRIC_LABLES.get('DiskDeviceLetter')}\"\n" +
                "  },\n" +
                "  \"value\":${METRIC_VALUE},\n" +
                "  \"ts\":${METRIC_TIME}\n" +
                "}";
        return msg;
    }

    public String getReceiverUuid() {
        return receiverUuid;
    }

    public void setReceiverUuid(String receiverUuid) {
        this.receiverUuid = receiverUuid;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getLabelsJsonStr() {
        return labelsJsonStr;
    }

    public void setLabelsJsonStr(String labelsJsonStr) {
        this.labelsJsonStr = labelsJsonStr;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
