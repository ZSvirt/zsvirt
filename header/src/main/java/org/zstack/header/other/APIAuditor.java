package org.zstack.header.other;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;

public interface APIAuditor {
    class Result {
        public String resourceUuid;
        public Class resourceType;

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

        public Result(String resourceUuid, Class resourceType) {
            this.resourceUuid = resourceUuid;
            if (this.resourceUuid == null) {
                this.resourceUuid = "";
            }

            this.resourceType = resourceType;
        }
    }

    Result audit(APIMessage msg, APIEvent rsp);
}
