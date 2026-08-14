package org.zstack.header.baremetal.instance;

import javax.persistence.*;

@Entity
@Table
public class BaremetalInstanceSequenceNumberVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
