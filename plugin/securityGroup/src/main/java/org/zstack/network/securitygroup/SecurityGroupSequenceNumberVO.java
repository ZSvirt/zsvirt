package org.zstack.network.securitygroup;

import javax.persistence.*;

@Entity
@Table
public class SecurityGroupSequenceNumberVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
