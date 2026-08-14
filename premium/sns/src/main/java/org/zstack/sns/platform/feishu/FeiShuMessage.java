package org.zstack.sns.platform.feishu;

import java.util.List;

public class FeiShuMessage {

    public static final String testMsg = "test message";
    public static class At {
        public List<String> atUserIds;
        public boolean isAtAll;
    }

    public static class Text {
        public String text;

        public Text(String text) {
            this.text = text;
        }

        public Text() {
        }
    }

    public String msg_type = "text";
    public Text content;
    public int timestamp;
    public String sign;
    public At at = new At();
}
