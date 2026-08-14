package org.zstack.monitoring.actions;

import org.zstack.header.vo.ForeignKey;
import org.zstack.monitoring.media.MediaVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * Created by xing5 on 2017/7/7.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class EmailTriggerActionVO extends MonitorTriggerActionVO {
    @Column
    private String email;
    @Column
    @ForeignKey(parentEntityClass = MediaVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String mediaUuid;

    public EmailTriggerActionVO() {
    }

    public EmailTriggerActionVO(MonitorTriggerActionVO other) {
        super(other);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMediaUuid() {
        return mediaUuid;
    }

    public void setMediaUuid(String mediaUuid) {
        this.mediaUuid = mediaUuid;
    }
}
