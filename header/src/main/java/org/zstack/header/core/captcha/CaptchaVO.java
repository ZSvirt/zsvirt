package org.zstack.header.core.captcha;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Created by kayo on 2018/7/5.
 */
@Entity
@Table
@BaseResource
public class CaptchaVO extends ResourceVO {
    @Column
    private String captcha;

    @Column
    private String verifyCode;

    @Column
    private String targetResourceIdentity;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public String getTargetResourceIdentity() {
        return targetResourceIdentity;
    }

    public void setTargetResourceIdentity(String targetResourceIdentity) {
        this.targetResourceIdentity = targetResourceIdentity;
    }
}
