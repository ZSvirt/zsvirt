package org.zstack.zwatch.thirdparty.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO;

/**
 * Create by yaoning at 2020/07/18
 */
@RestRequest(
        path = "/zwatch/third-party/platforms",
        method = HttpMethod.POST,
        responseClass = APIAddThirdpartyPlatformEvent.class,
        parameterName = "params"
)
public class APIAddThirdpartyPlatformMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 255)
    private String type;

    @NoLogging
    @APIParam(maxLength = 2048)
    private String url;

    @APIParam(maxLength = 8192)
    private String template;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    public static APIAddThirdpartyPlatformMsg __example__() {
        APIAddThirdpartyPlatformMsg msg = new APIAddThirdpartyPlatformMsg();
        msg.setName("xsky-172.20.11.24");
        msg.setType("XSKY");
        msg.setUrl("http://172.20.11.24:8086/alerts/?token=001adb2ef25e41b7bd01b28651fcfa6a");
        String template = "{\n" +
                "    \"product\":\"XSKY\",\n" +
                "    \"service\":\"XSKY\",\n" +
                "    \"message\":\"${resource_type + '[' + resource_name+'] ' + group + ' ' + alert_value}\",\n" +
                "    \"metric\":\"${resource_type + '::' + group}\",\n" +
                "    \"alertLevel\":\"${level == 'info' ? 'Normal' : level == 'warning' ? 'Important' : 'Emergent'}\",\n" +
                "    \"alertTime\":\"${create}\",\n" +
                "    \"dimensions\":\"{'resource_name':'${resource_name}'}\",\n" +
                "    \"dataSource\":\"xsky-172.20.196.185\"\n" +
                "}";
        msg.setTemplate(template);
        msg.setDescription("desc");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddThirdpartyPlatformEvent)rsp).getInventory().getUuid() : "", ThirdpartyPlatformVO.class);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
