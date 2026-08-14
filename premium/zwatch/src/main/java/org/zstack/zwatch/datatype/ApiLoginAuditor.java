package org.zstack.zwatch.datatype;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIReply;

/**
 * author:kaicai.hu
 * Date:2019/4/18
 */
public abstract class ApiLoginAuditor extends ApiAuditor {
    public ApiLoginAuditor(Class apiClass) {
        super(apiClass);
    }

    public static class LoginResult extends Result {
        public String clientIp;
        public String clientBrowser;

        public LoginResult(String clientIp, String clientBrowser, String resourceUuid, Class resourceType) {
            super(resourceUuid, resourceType);
            this.clientIp = clientIp;
            this.clientBrowser = clientBrowser;
        }
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return null;
    }

    public abstract LoginResult loginAuditor(APIMessage msg, APIReply reply);
}
