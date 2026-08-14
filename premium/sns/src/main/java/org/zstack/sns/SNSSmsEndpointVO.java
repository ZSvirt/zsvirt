package org.zstack.sns;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Qi Le on 2019-07-10
 */
@Entity
@Table
public class SNSSmsEndpointVO extends SNSApplicationEndpointVO {
    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "endpointUuid")
    private Set<SNSSmsReceiverVO> receivers = new HashSet<>();

    public SNSSmsEndpointVO() {
    }

    public SNSSmsEndpointVO(SNSApplicationEndpointVO other) {
        super(other);
    }

    public Set<SNSSmsReceiverVO> getReceivers() {
        return receivers;
    }

    public void setReceivers(Set<SNSSmsReceiverVO> receivers) {
        this.receivers = receivers;
    }
}
