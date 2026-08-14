package org.zstack.header.keyprovider;

import org.zstack.header.tag.AutoDeleteTag;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@AutoDeleteTag
public class NkpVO extends KeyProviderVO {
    @Column
    private String kdf;

    @Column
    private String saltPolicy;

    @Column
    private boolean backedUp;

    @Column
    private Integer currentVersion;

    public String getKdf() {
        return kdf;
    }

    public void setKdf(String kdf) {
        this.kdf = kdf;
    }

    public String getSaltPolicy() {
        return saltPolicy;
    }

    public void setSaltPolicy(String saltPolicy) {
        this.saltPolicy = saltPolicy;
    }

    public boolean isBackedUp() {
        return backedUp;
    }

    public void setBackedUp(boolean backedUp) {
        this.backedUp = backedUp;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

}
