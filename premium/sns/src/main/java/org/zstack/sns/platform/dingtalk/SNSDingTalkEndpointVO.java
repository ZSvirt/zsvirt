package org.zstack.sns.platform.dingtalk;

import org.zstack.sns.SNSApplicationEndpointVO;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Table
@Entity
public class SNSDingTalkEndpointVO extends SNSApplicationEndpointVO {
    @Column
    private String url;

    @Column
    private boolean atAll;

    @Column
    private String secret;

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "endpointUuid")
    private Set<SNSDingTalkAtPersonVO> atPersons = new HashSet<>();

    public SNSDingTalkEndpointVO() {
    }

    public SNSDingTalkEndpointVO(SNSApplicationEndpointVO other) {
        super(other);
    }

    public Set<SNSDingTalkAtPersonVO> getAtPersons() {
        return atPersons;
    }

    public void setAtPersons(Set<SNSDingTalkAtPersonVO> atPersons) {
        this.atPersons = atPersons;
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

    public String getSecret() {
        return secret;
    }

    public void setSecret(String dingTalkSign) {
        this.secret = dingTalkSign;
    }
}
