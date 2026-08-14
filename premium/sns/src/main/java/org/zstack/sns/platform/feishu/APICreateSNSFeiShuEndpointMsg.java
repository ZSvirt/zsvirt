package org.zstack.sns.platform.feishu;

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

@RestRequest(path = "/sns/application-endpoints/feishu",
        method = HttpMethod.POST,
        responseClass = APICreateSNSFeiShuEndpointEvent.class,
        parameterName = "params")
public class APICreateSNSFeiShuEndpointMsg extends APICreateSNSApplicationEndpointMsg implements SNSApplicationPlatformMessage {
    @APIParam(maxLength = 2048)
    private String url;
    @APIParam(required = false)
    private Boolean atAll;
    @APIParam(nonempty = true, required = false)
    private List<String> atPersonUserIds;
    @APIParam(maxLength = 128, required = false)
    private String secret;
    @APIParam(nonempty = true, required = false)
    private Map<String, String> atPersonList;

    public static APICreateSNSFeiShuEndpointMsg __example__() {
        APICreateSNSFeiShuEndpointMsg msg = new APICreateSNSFeiShuEndpointMsg();
        msg.setName("feishu");
        msg.setUrl("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=f8b9014a-207a-44d5-ae26-3501bf01dbc4");
        msg.setAtAll(false);
        msg.setAtPersonUserIds(asList("18900002222", "13377778888"));
        msg.setSecret("DPKfmrIhYXY5M2TWGXy0ed");
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

    public List<String> getAtPersonUserIds() {
        return atPersonUserIds;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setAtPersonUserIds(List<String> atPersonUserIds) {
        this.atPersonUserIds = atPersonUserIds;
    }

    public Map<String, String> getAtPersonList() {
        return atPersonList;
    }

    public void setAtPersonList(Map<String, String> atPersonList) {
        this.atPersonList = atPersonList;
    }

    @Override
    public String getApplicationEndpointType() {
        return SNSFeiShuEndpointFactory.type.toString();
    }
}
