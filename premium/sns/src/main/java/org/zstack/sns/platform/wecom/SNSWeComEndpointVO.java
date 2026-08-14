package org.zstack.sns.platform.wecom;

import org.zstack.sns.SNSApplicationEndpointVO;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Table
@Entity
public class SNSWeComEndpointVO extends SNSApplicationEndpointVO {
    @Column
    private String url;

    @Column
    private boolean atAll;

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "endpointUuid")
    private Set<SNSWeComAtPersonVO> atPersons = new HashSet<>();

    public SNSWeComEndpointVO() {
    }

    public SNSWeComEndpointVO(SNSApplicationEndpointVO other) {
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

    public Set<SNSWeComAtPersonVO> getAtPersons() {
        return atPersons;
    }

    public void setAtPersons(Set<SNSWeComAtPersonVO> atPersons) {
        this.atPersons = atPersons;
    }
}
