package org.zstack.zwatch.alarm.sns.template.aliyunsms;

import org.zstack.zwatch.alarm.sns.SNSTextTemplateVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * Created by Qi Le on 2019-07-12
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class AliyunSmsSNSTextTemplateVO extends SNSTextTemplateVO {
    @Column
    private String sign;
    @Column
    private String alarmTemplateCode;
    @Column
    private String eventTemplateCode;
    @Column
    private String eventTemplate;

    public String getAlarmTemplateCode() {
        return alarmTemplateCode;
    }

    public void setAlarmTemplateCode(String alarmTemplateCode) {
        this.alarmTemplateCode = alarmTemplateCode;
    }

    public String getEventTemplateCode() {
        return eventTemplateCode;
    }

    public void setEventTemplateCode(String eventTemplateCode) {
        this.eventTemplateCode = eventTemplateCode;
    }

    public String getEventTemplate() {
        return eventTemplate;
    }

    public void setEventTemplate(String eventTemplate) {
        this.eventTemplate = eventTemplate;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}
