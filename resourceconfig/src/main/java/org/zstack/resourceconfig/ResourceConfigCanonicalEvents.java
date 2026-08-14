package org.zstack.resourceconfig;

import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by MaJin on 2019/2/23.
 */
public class ResourceConfigCanonicalEvents {
    public static final String UPDATE_EVENT_PATH = "/resourceConfig/update/{category}/{name}/{nodeUuid}";
    public static final String DELETE_EVENT_PATH = "/resourceConfig/delete/{category}/{name}/{nodeUuid}";

    @NeedJsonSchema
    public static class UpdateEvent {
        private String oldValue;
        private String resourceUuid;
        private String resourceType;

        public String getOldValue() {
            return oldValue;
        }

        public void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }
    }

    @NeedJsonSchema
    public static class DeleteEvent {
        private String oldValue;
        private String resourceUuid;
        private String resourceType;

        public String getOldValue() {
            return oldValue;
        }

        public void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }
    }
}
