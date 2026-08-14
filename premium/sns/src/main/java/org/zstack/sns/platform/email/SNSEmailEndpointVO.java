package org.zstack.sns.platform.email;

import org.zstack.sns.SNSApplicationEndpointVO;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class SNSEmailEndpointVO extends SNSApplicationEndpointVO {
    @Column
    private String email;
    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "endpointUuid")
    private Set<SNSEmailAddressVO> emailAddresses = new HashSet<>();

    public SNSEmailEndpointVO(SNSApplicationEndpointVO other) {
        super(other);
    }

    public SNSEmailEndpointVO() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<SNSEmailAddressVO> getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(Set<SNSEmailAddressVO> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }
}
