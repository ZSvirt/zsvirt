package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/sns/topics/{uuid}/actions", method = HttpMethod.PUT, responseClass = APIUpdateSNSTopicEvent.class, isAction = true)
public class APIUpdateSNSTopicMsg extends APIMessage implements SNSTopicMessage {
    @APIParam(resourceType = SNSTopicVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 32, required = false, validValues = {"zh_CN", "en_US"})
    private String locale;

    public static APIUpdateSNSTopicMsg __example__() {
        APIUpdateSNSTopicMsg msg = new APIUpdateSNSTopicMsg();
        msg.setUuid(uuid());
        msg.setName("new name");
        msg.setLocale("zh_CN");
        return msg;
    }

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

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public String getTopicUuid() {
        return uuid;
    }
}
