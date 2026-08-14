package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/sns/topics", method = HttpMethod.POST, responseClass = APICreateSNSTopicEvent.class,
        parameterName = "params")
public class APICreateSNSTopicMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 32, required = false, validValues = {"zh_CN", "en_US"})
    private String locale;

    public static APICreateSNSTopicMsg __example__() {
        APICreateSNSTopicMsg msg = new APICreateSNSTopicMsg();
        msg.setName("api topic");
        msg.setLocale("zh_CN");
        return msg;
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
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateSNSTopicEvent)rsp).getInventory().getUuid() : "", SNSTopicVO.class);
    }
}
