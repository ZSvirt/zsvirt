package org.zstack.core.config;

import org.zstack.header.message.NeedJsonSchema;

/**
 */
public class GlobalConfigCanonicalEvents {
    public static final String UPDATE_EVENT_PATH = "/globalConfig/update/{category}/{name}/{nodeUuid}";

    @NeedJsonSchema
    public static class UpdateEvent {
        private String oldValue;
        private String newValue;

        public String getOldValue() {
            return oldValue;
        }

        public void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }

        /**
            new value should be read from database to prevent from dirty read when multi nodes
         */
        @Deprecated
        public String getNewValue() {
            return newValue;
        }

        public void setNewValue(String newValue) {
            this.newValue = newValue;
        }
    }
}
