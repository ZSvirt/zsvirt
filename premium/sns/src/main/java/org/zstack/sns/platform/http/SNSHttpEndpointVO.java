package org.zstack.sns.platform.http;

import org.zstack.sns.SNSApplicationEndpointVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
public class SNSHttpEndpointVO extends SNSApplicationEndpointVO {
    @Column
    private String url;
    @Column
    private String username;
    @Column
    private String password;

    public SNSHttpEndpointVO() {
    }

    public SNSHttpEndpointVO(SNSApplicationEndpointVO other) {
        super(other);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
