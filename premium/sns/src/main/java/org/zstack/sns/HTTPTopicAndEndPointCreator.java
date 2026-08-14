package org.zstack.sns;

import org.zstack.core.Platform;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.sns.platform.http.SNSHttpEndpointVO;
import org.zstack.sns.platform.http.SNSHttpEndpointVO_;
import org.zstack.sns.platform.http.SNSSystemHttpEndpointFactory;
import org.zstack.utils.DebugUtils;

import java.util.Objects;

public class HTTPTopicAndEndPointCreator {
    private String topicUuid;
    private String topicName;
    private String topicDescription;
    private String endPointName;
    private String endPointDescription;
    private String httpURL;
    private String httpUsername;
    private String httpPassword;

    public void mergeByName() {
        DebugUtils.Assert(endPointName != null, "endPointName cannot be null !!");

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (!q(SNSTopicVO.class).eq(SNSTopicVO_.uuid, topicUuid).isExists()) {
                    SNSTopicVO topic = new SNSTopicVO();
                    topic.setUuid(topicUuid);
                    topic.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    topic.setName(topicName == null ? "created-by-SystemHTTPTopicAndEndpointCreator" : topicName);
                    topic.setDescription(topicDescription);
                    topic.setState(SNSTopicState.Enabled);
                    persist(topic);
                } else {
                    sql(SNSTopicVO.class).eq(SNSTopicVO_.uuid, topicUuid).set(SNSTopicVO_.ownerType, null).update();
                }

                SNSHttpEndpointVO endpoint = q(SNSHttpEndpointVO.class)
                        .eq(SNSHttpEndpointVO_.name, endPointName)
                        .eq(SNSHttpEndpointVO_.description, endPointDescription)
                        .isNull(SNSHttpEndpointVO_.ownerType).find();
                if (endpoint == null) {
                    endpoint = new SNSHttpEndpointVO();
                    endpoint.setUuid(Platform.getUuid());
                    endpoint.setUrl(httpURL);
                    endpoint.setUsername(httpUsername);
                    endpoint.setPlatformUuid(SNSConstants.SYSTEM_PLATFORM_UUID);
                    endpoint.setPassword(httpPassword);
                    endpoint.setState(SNSApplicationEndpointState.Enabled);
                    endpoint.setType(SNSSystemHttpEndpointFactory.type.toString());
                    endpoint.setName(endPointName);
                    endpoint.setDescription(endPointDescription);
                    endpoint.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    persist(endpoint);
                } else if (!Objects.equals(endpoint.getUrl(), httpURL) ||
                        !Objects.equals(endpoint.getUsername(), httpUsername) ||
                        !Objects.equals(endpoint.getPassword(), httpPassword)){
                    endpoint.setUrl(httpURL);
                    endpoint.setUsername(httpUsername);
                    endpoint.setPassword(httpPassword);
                    merge(endpoint);
                }

                if (!q(SNSSubscriberVO.class).eq(SNSSubscriberVO_.endpointUuid, endpoint.getUuid())
                        .eq(SNSSubscriberVO_.topicUuid, topicUuid).isExists()) {
                    SNSSubscriberVO s = new SNSSubscriberVO();
                    s.setTopicUuid(topicUuid);
                    s.setEndpointUuid(endpoint.getUuid());
                    persist(s);
                }

                boolean exists = q(AccountResourceRefVO.class)
                        .eq(AccountResourceRefVO_.resourceUuid, endpoint.getUuid())
                        .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                        .isExists();
                if (!exists) {
                    AccountResourceRefVO svo = new AccountResourceRefVO();
                    svo.setResourceType(SNSApplicationEndpointVO.class.getSimpleName());
                    svo.setResourceUuid(endpoint.getUuid());
                    svo.setType(AccessLevel.SharePublic);
                    persist(svo);
                }

                exists = q(AccountResourceRefVO.class)
                        .eq(AccountResourceRefVO_.resourceUuid, topicUuid)
                        .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                        .isExists();
                if (!exists) {
                    AccountResourceRefVO svo = new AccountResourceRefVO();
                    svo.setResourceType(SNSTopicVO.class.getSimpleName());
                    svo.setResourceUuid(topicUuid);
                    svo.setType(AccessLevel.SharePublic);
                    persist(svo);
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

    public String getEndPointName() {
        return endPointName;
    }

    public void setEndPointName(String endPointName) {
        this.endPointName = endPointName;
    }

    public String getEndPointDescription() {
        return endPointDescription;
    }

    public void setEndPointDescription(String endPointDescription) {
        this.endPointDescription = endPointDescription;
    }

    public static final class Builder {
        private String topicUuid;
        private String topicName;
        private String topicDescription;
        private String httpURL;
        private String httpUsername;
        private String httpPassword;
        private String endpointName;
        private String endpointDescription;

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

        public Builder topicDescription(String topicDescription) {
            this.topicDescription = topicDescription;
            return this;
        }

        public Builder endpointName(String endpointName) {
            this.endpointName = endpointName;
            return this;
        }

        public Builder endpointDescription(String endpointDescription) {
            this.endpointDescription = endpointDescription;
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

        public HTTPTopicAndEndPointCreator build() {
            HTTPTopicAndEndPointCreator creator = new HTTPTopicAndEndPointCreator();
            creator.setTopicUuid(topicUuid);
            creator.setTopicName(topicName);
            creator.setTopicDescription(topicDescription);
            creator.setHttpURL(httpURL);
            creator.setHttpUsername(httpUsername);
            creator.setHttpPassword(httpPassword);
            creator.setEndPointName(endpointName);
            creator.setEndPointDescription(endpointDescription);
            return creator;
        }
    }
}
