package org.zstack.sns.platform.dingtalk;

import java.util.List;

public class DingTalkMessage {

    public static final String DEFAULT_TITLE = "test connected";
    public static final String testMsg = "test message";

    public static class At {
        public List<String> atMobiles;
        public boolean isAtAll;
    }

    public static class Markdown {
        public String text;
        public String title;

        public Markdown(String text, String title) {
            this.text = text;
            this.title = title;
        }

        public Markdown(String text) {
            this.title = DEFAULT_TITLE;
            this.text = text;
        }

        public Markdown() {
        }
    }

    public Markdown markdown;
    public String msgtype = "markdown";
    public At at = new At();
}
