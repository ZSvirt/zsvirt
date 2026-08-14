package org.zstack.sns.platform.email;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APICreateSNSApplicationEndpointEvent;
import org.zstack.sns.APICreateSNSApplicationEndpointMsg;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/sns/application-endpoints/emails",
        method = HttpMethod.POST,
        responseClass = APICreateSNSApplicationEndpointEvent.class,
        parameterName = "params"
)
public class APICreateSNSEmailEndpointMsg extends APICreateSNSApplicationEndpointMsg {
    @Deprecated
    @APIParam(maxLength = 1024, required = false)
    private String email;

    @APIParam(required = false)
    private List<String> emails;

    public static APICreateSNSEmailEndpointMsg __example__() {
        APICreateSNSEmailEndpointMsg msg = new APICreateSNSEmailEndpointMsg();
        msg.setName("email");
        msg.setPlatformUuid(uuid());
        msg.setEmail("example@zstack.io");
        msg.setEmails(Collections.singletonList("example@zstack.io"));
        return msg;
    }

    @Deprecated
    public String getEmail() {
        return email;
    }

    @Deprecated
    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }
}
