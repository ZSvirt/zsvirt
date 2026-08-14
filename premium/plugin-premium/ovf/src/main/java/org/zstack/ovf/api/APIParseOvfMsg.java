package org.zstack.ovf.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by Qi Le on 2022/3/3
 */
@RestRequest(
        path = "/ovf/parse",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIParseOvfReply.class
)
public class APIParseOvfMsg extends APISyncCallMessage {
    @APIParam
    private String xmlBase64;

    public String getXmlBase64() {
        return xmlBase64;
    }

    public void setXmlBase64(String xmlBase64) {
        this.xmlBase64 = xmlBase64;
    }

    public static APIParseOvfMsg __example__() {
        APIParseOvfMsg msg = new APIParseOvfMsg();
        msg.setXmlBase64("Base64 encoded ovf file content.");
        return msg;
    }
}
