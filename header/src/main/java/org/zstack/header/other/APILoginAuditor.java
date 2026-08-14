package org.zstack.header.other;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIReply;

/**
 * author:kaicai.hu
 * Date:2019/4/18
 */
public interface APILoginAuditor {
    class LoginResult {
        public String clientIp;
        public String clientBrowser;
        public String resourceUuid;
        public Class resourceType;

        public String getClientIp() {
            return clientIp;
        }

        public void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }

        public String getClientBrowser() {
            return clientBrowser;
        }

        public void setClientBrowser(String clientBrowser) {
            this.clientBrowser = clientBrowser;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public Class getResourceType() {
            return resourceType;
        }

        public void setResourceType(Class resourceType) {
            this.resourceType = resourceType;
        }

        public LoginResult(String clientIp, String clientBrowser, String resourceUuid, Class resourceType) {
            this.clientIp = clientIp;
            this.clientBrowser = clientBrowser;
            this.resourceUuid = resourceUuid;
            if (this.resourceUuid == null) {
                this.resourceUuid = "";
            }
            this.resourceType = resourceType;
        }
    }

    LoginResult loginAudit(APIMessage msg, APIReply reply);
}
