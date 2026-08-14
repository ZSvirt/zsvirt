package org.zstack.sns.system;

import org.zstack.core.Platform;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.identity.AccountConstant;
import org.zstack.sns.*;
import org.zstack.sns.platform.http.SNSHttpEndpointFactory;
import org.zstack.sns.platform.http.SNSHttpEndpointVO;
import org.zstack.sns.platform.http.SNSHttpEndpointVO_;

public class SystemHTTPTopicAndEndpointCreator {
    private String topicUuid;
    private String topicName;
    private String topicDescription;
    private String httpURL;
    private String httpUsername;
    private String httpPassword;

    private static String systemHttpEndpointName = "created-by-SystemHTTPTopicAndEndpointCreator";

    public void create() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (!q(SNSTopicVO.class).eq(SNSTopicVO_.uuid, topicUuid)
                        .eq(SNSTopicVO_.ownerType, SNSTopicOwnerType.System).isExists()) {
                    SNSTopicVO topic = new SNSTopicVO();
                    topic.setUuid(topicUuid);
                    topic.setOwnerType(SNSTopicOwnerType.System);
                    topic.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    topic.setName(topicName == null ? systemHttpEndpointName : topicName);
                    topic.setDescription(topicDescription);
                    topic.setState(SNSTopicState.Enabled);
                    persist(topic);
                }

                String endpointUuid;
                if (!q(SNSHttpEndpointVO.class).eq(SNSHttpEndpointVO_.url, httpURL).eq(SNSHttpEndpointVO_.name, systemHttpEndpointName)
                        .eq(SNSHttpEndpointVO_.ownerType, SNSApplicationEndpointOwnerType.System).isExists()) {
                    SNSHttpEndpointVO e = new SNSHttpEndpointVO();
                    e.setUuid(Platform.getUuid());
                    e.setUrl(httpURL);
                    e.setUsername(httpUsername);
                    e.setPlatformUuid(SNSConstants.SYSTEM_PLATFORM_UUID);
                    e.setPassword(httpPassword);
                    e.setState(SNSApplicationEndpointState.Enabled);
                    e.setType(SNSHttpEndpointFactory.type.toString());
                    e.setOwnerType(SNSApplicationEndpointOwnerType.System);
                    e.setName(systemHttpEndpointName);
                    e.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    persist(e);

                    endpointUuid = e.getUuid();
                } else {
                    endpointUuid = q(SNSHttpEndpointVO.class).select(SNSHttpEndpointVO_.uuid)
                            .eq(SNSHttpEndpointVO_.name, systemHttpEndpointName)
                            .eq(SNSHttpEndpointVO_.url, httpURL)
                            .eq(SNSHttpEndpointVO_.ownerType, SNSApplicationEndpointOwnerType.System)
                            .findValue();
                }

                if (!q(SNSSubscriberVO.class).eq(SNSSubscriberVO_.endpointUuid, endpointUuid)
                        .eq(SNSSubscriberVO_.topicUuid, topicUuid).isExists()) {
                    SNSSubscriberVO s = new SNSSubscriberVO();
                    s.setTopicUuid(topicUuid);
                    s.setEndpointUuid(endpointUuid);
                    persist(s);
                }
            }
        }.execute();
    }

    public String getTopicUuid() {
        return topicUuid;
    }

    public void setTopicUuid(String topicUuid) {
        this.topicUuid = topicUuid;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicDescription() {
        return topicDescription;
    }

    public void setTopicDescription(String topicDescription) {
        this.topicDescription = topicDescription;
    }

    public String getHttpURL() {
        return httpURL;
    }

    public void setHttpURL(String httpURL) {
        this.httpURL = httpURL;
    }

    public String getHttpUsername() {
        return httpUsername;
    }

    public void setHttpUsername(String httpUsername) {
        this.httpUsername = httpUsername;
    }

    public String getHttpPassword() {
        return httpPassword;
    }

    public void setHttpPassword(String httpPassword) {
        this.httpPassword = httpPassword;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String topicUuid;
        private String topicName;
        private String topicDescription;
        private String httpURL;
        private String httpUsername;
        private String httpPassword;

        private Builder() {
        }

        public Builder topicUuid(String topicUuid) {
            this.topicUuid = topicUuid;
            return this;
        }

        public Builder topicName(String topicName) {
            this.topicName = topicName;
            return this;
        }

        public Builder topicDesciption(String topicDesciption) {
            this.topicDescription = topicDesciption;
            return this;
        }

        public Builder httpURL(String httpURL) {
            this.httpURL = httpURL;
            return this;
        }

        public Builder httpUsername(String httpUsername) {
            this.httpUsername = httpUsername;
            return this;
        }

        public Builder httpPassword(String httpPassword) {
            this.httpPassword = httpPassword;
            return this;
        }

        public SystemHTTPTopicAndEndpointCreator build() {
            SystemHTTPTopicAndEndpointCreator systemHTTPTopicAndEndpointCreator = new SystemHTTPTopicAndEndpointCreator();
            systemHTTPTopicAndEndpointCreator.setTopicUuid(topicUuid);
            systemHTTPTopicAndEndpointCreator.setTopicName(topicName);
            systemHTTPTopicAndEndpointCreator.setTopicDescription(topicDescription);
            systemHTTPTopicAndEndpointCreator.setHttpURL(httpURL);
            systemHTTPTopicAndEndpointCreator.setHttpUsername(httpUsername);
            systemHTTPTopicAndEndpointCreator.setHttpPassword(httpPassword);
            return systemHTTPTopicAndEndpointCreator;
        }
    }
}
