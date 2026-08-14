package org.zstack.sns.platform.dingtalk;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APICreateSNSApplicationEndpointMsg;
import org.zstack.sns.SNSApplicationPlatformMessage;
import org.zstack.sns.SNSConstants;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

@RestRequest(path = "/sns/application-endpoints/ding-talk",
        method = HttpMethod.POST,
        responseClass = APICreateSNSDingTalkEndpointEvent.class,
        parameterName = "params")
public class APICreateSNSDingTalkEndpointMsg extends APICreateSNSApplicationEndpointMsg implements SNSApplicationPlatformMessage {
    @APIParam(maxLength = 2048)
    private String url;
    @APIParam(required = false)
    private Boolean atAll;
    @APIParam(maxLength = 128, required = false)
    private String secret;
    @APIParam(nonempty = true, required = false)
    private List<String> atPersonPhoneNumbers;
    @APIParam(nonempty = true, required = false)
    private Map<String, String> atPersonList;

    public static APICreateSNSDingTalkEndpointMsg __example__() {
        APICreateSNSDingTalkEndpointMsg msg = new APICreateSNSDingTalkEndpointMsg();
        msg.setName("dinding");
        msg.setUrl("http://dingding-robot-url");
        msg.setAtAll(false);
        msg.setSecret("SECca7c224f47ab16fbe51050ae0b8ebfc505b2b866fc0eb3768c8d79527d1bacc0");
        msg.setAtPersonPhoneNumbers(asList("18900002222", "13377778888"));
        msg.setAtPersonList(map(e("13377778888", "jack")));
        return msg;
    }

    @Override
    public String getPlatformUuid() {
        return SNSConstants.SYSTEM_PLATFORM_UUID;
    }

    @Override
    public String getApplicationPlatformUuid() {
        return getPlatformUuid();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getAtAll() {
        return atAll;
    }

    public void setAtAll(Boolean atAll) {
        this.atAll = atAll;
    }

    public List<String> getAtPersonPhoneNumbers() {
        return atPersonPhoneNumbers;
    }

    public void setAtPersonPhoneNumbers(List<String> atPersonPhoneNumbers) {
        this.atPersonPhoneNumbers = atPersonPhoneNumbers;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Map<String, String> getAtPersonList() {
        return atPersonList;
    }

    public void setAtPersonList(Map<String, String> atPersonList) {
        this.atPersonList = atPersonList;
    }

    @Override
    public String getApplicationEndpointType() {
        return SNSDingTalkEndpointFactory.type.toString();
    }
}
