package org.zstack.header.baremetal.preconfiguration;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * Created by GuoYi on 2018-12-26.
 */
@TagResourceType(PreconfigurationTemplateVO.class)
@RestRequest(
        path = "/baremetal/preconfigurations",
        method = HttpMethod.POST,
        responseClass = APIAddPreconfigurationTemplateEvent.class,
        parameterName = "params"
)
public class APIAddPreconfigurationTemplateMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(maxLength = 64)
    private String distribution;

    @APIParam(validEnums = PreconfigurationTemplateType.class)
    private String type;

    @APIParam(maxLength = PreconfigurationConstant.contentMaxLength)
    private String content;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddPreconfigurationTemplateEvent)rsp).getInventory().getUuid() : "", PreconfigurationTemplateVO.class);
    }

    public static APIAddPreconfigurationTemplateMsg __example__() {
        APIAddPreconfigurationTemplateMsg msg = new APIAddPreconfigurationTemplateMsg();
        msg.setName("centos-7.2-minimal");
        msg.setDescription("centos-7.2-minimal kickstart file");
        msg.setDistribution("centos7.2-x86_64");
        msg.setType(PreconfigurationTemplateType.kickstart.toString());
        msg.setContent("...");
        return msg;
    }
}
