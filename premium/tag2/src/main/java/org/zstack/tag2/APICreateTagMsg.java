package org.zstack.tag2;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagPatternVO;

@RestRequest(
        path = "/tags",
        method = HttpMethod.POST,
        responseClass = APICreateTagEvent.class,
        parameterName = "params"
)
public class APICreateTagMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 128)
    private String name;

    @APIParam(maxLength = 128)
    private String value;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false)
    private String color;

    @APIParam(required = false, validValues = {"simple", "withToken"})
    private String type = "simple";

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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

    public static APICreateTagMsg __example__() {
        APICreateTagMsg msg = new APICreateTagMsg();
        msg.name = "new-tag";
        msg.value = "new-tag";
        msg.color = "#FFFFFF";
        msg.description = "tag-for-volume";
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateTagEvent)rsp).getInventory().getUuid() : "", TagPatternVO.class);
    }
}
