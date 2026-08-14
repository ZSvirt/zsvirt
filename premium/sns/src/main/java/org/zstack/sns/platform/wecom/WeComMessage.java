package org.zstack.sns.platform.wecom;

import java.util.List;

public class WeComMessage {

    public static final String testMsg = "test message";
    public static class At {
        public List<String> atUserIds;
        public boolean isAtAll;
    }

    public static class Markdown {
        public String content;

        public Markdown(String content) {
            this.content = content;
        }

        public Markdown() {
        }
    }

    public String msgtype = "markdown";
    public Markdown markdown;
    public At at = new At();
}
