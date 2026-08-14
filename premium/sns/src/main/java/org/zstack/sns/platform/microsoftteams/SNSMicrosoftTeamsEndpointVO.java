package org.zstack.sns.platform.microsoftteams;

import org.zstack.sns.SNSApplicationEndpointVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Table
@Entity
public class SNSMicrosoftTeamsEndpointVO extends SNSApplicationEndpointVO {
    @Column
    private String url;

    public SNSMicrosoftTeamsEndpointVO() {
    }

    public SNSMicrosoftTeamsEndpointVO(SNSApplicationEndpointVO other) {
        super(other);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
