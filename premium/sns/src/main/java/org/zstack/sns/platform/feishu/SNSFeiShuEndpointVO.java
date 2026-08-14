package org.zstack.sns.platform.feishu;

import org.zstack.sns.SNSApplicationEndpointVO;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Table
@Entity
public class SNSFeiShuEndpointVO extends SNSApplicationEndpointVO {
    @Column
    private String url;

    @Column
    private boolean atAll;

    @Column
    private String secret;

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "endpointUuid")
    private Set<SNSFeiShuAtPersonVO> atPersons = new HashSet<>();

    public SNSFeiShuEndpointVO() {
    }

    public SNSFeiShuEndpointVO(SNSApplicationEndpointVO other) {
        super(other);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isAtAll() {
        return atAll;
    }

    public void setAtAll(boolean atAll) {
        this.atAll = atAll;
    }

    public Set<SNSFeiShuAtPersonVO> getAtPersons() {
        return atPersons;
    }

    public void setAtPersons(Set<SNSFeiShuAtPersonVO> atPersons) {
        this.atPersons = atPersons;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String feiShuSecret) {
        this.secret = feiShuSecret;
    }
}
