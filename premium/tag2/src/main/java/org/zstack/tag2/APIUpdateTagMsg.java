package org.zstack.tag2;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagPatternVO;

@RestRequest(
        path = "/tags/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateTagEvent.class
)
public class APIUpdateTagMsg extends APIMessage implements TagPatternMessage {
    @APIParam(resourceType = TagPatternVO.class)
    private String uuid;

    @APIParam(required = false, maxLength = 128)
    private String name;

    @APIParam(required = false, maxLength = 128)
    private String value;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false)
    private String color;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    @Override
    public String getTagPatternUuid() {
        return uuid;
    }

    public static APIUpdateTagMsg __example__() {
        APIUpdateTagMsg msg = new APIUpdateTagMsg();
        msg.uuid = uuid(TagPatternVO.class);
        msg.value = "new-tag";
        msg.color = "#FFFFFF";
        msg.description = "tag-for-volume";
        return msg;
    }
}
