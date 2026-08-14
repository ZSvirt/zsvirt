package org.zstack.core.errorcode;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/12/1.
 */
@RestRequest(
        path = "/errorcode/elaborations",
        method = HttpMethod.GET,
        responseClass = APIGetElaborationsReply.class
)
public class APIGetElaborationsMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private String category;
    @APIParam(required = false)
    private String code;
    @APIParam(required = false)
    private String regex;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public static APIGetElaborationsMsg __example__() {
        APIGetElaborationsMsg msg = new APIGetElaborationsMsg();
        msg.setCategory("BS");
        msg.setRegex("certificate has expired or is not yet valid");

        return msg;
    }
}
