package org.zstack.sns.platform.microsoftteams;

public class MicrosoftTeamsMessage {

    public static final String DEFAULT_TITLE = "test connected";
    public static final String testMsg = "test message";

    public static class TeamsJson {
        public String text;
        public String title;

        public TeamsJson(String text, String title) {
            this.text = text;
            this.title = title;
        }

        public TeamsJson(String text) {
            this.title = DEFAULT_TITLE;
            this.text = text;
        }

        public TeamsJson() {
        }
    }

    public TeamsJson teamsJson;
}

