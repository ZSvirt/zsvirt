package org.zstack.header.baremetal.preconfiguration;

import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2018-12-26.
 */
@RestRequest(
        path = "/baremetal/preconfigurations/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdatePreconfigurationTemplateEvent.class,
        isAction = true
)
public class APIUpdatePreconfigurationTemplateMsg extends APIMessage implements PreconfigurationTemplateMessage {
    @APIParam(resourceType = PreconfigurationTemplateVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(maxLength = 64, required = false)
    private String distribution;

    @APIParam(validEnums = PreconfigurationTemplateType.class, required = false)
    private String type;

    @APIParam(maxLength = PreconfigurationConstant.contentMaxLength, required = false)
    private String content;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public static APIUpdatePreconfigurationTemplateMsg __example__() {
        APIUpdatePreconfigurationTemplateMsg msg = new APIUpdatePreconfigurationTemplateMsg();
        msg.setUuid(uuid(PreconfigurationTemplateVO.class));
        msg.setName("centos-7.2-minimal");
        msg.setDescription("centos-7.2-minimal kickstart file");
        msg.setDistribution("centos7.2-x86_64");
        msg.setType(PreconfigurationTemplateType.kickstart.toString());
        msg.setContent("...");
        return msg;
    }

    @Override
    public String getTemplateUuid() {
        return uuid;
    }
}
