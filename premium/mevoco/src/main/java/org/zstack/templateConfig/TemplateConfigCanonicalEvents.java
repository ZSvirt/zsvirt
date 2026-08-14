package org.zstack.templateConfig;

import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by LiangHanYu on 2022/4/21 14:59
 */
public class TemplateConfigCanonicalEvents {
    public static final String UPDATE_EVENT_PATH = "/templateConfig/update/{templateUuid}/{category}/{name}/{nodeUuid}";

    @NeedJsonSchema
    public static class TemplateConfigUpdateEvent {
        private String oldValue;
        private String newValue;

        public String getOldValue() {
            return oldValue;
        }

        public void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        public void setNewValue(String newValue) {
            this.newValue = newValue;
        }
    }
}

