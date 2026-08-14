package org.zstack.zwatch.alarm.sns.template.aliyunsms;

import org.zstack.header.search.Inventory;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Qi Le on 2019-07-12
 */
@Inventory(mappingVOClass = AliyunSmsSNSTextTemplateVO.class)
public class AliyunSmsSNSTextTemplateInventory extends SNSTextTemplateInventory {
    private String alarmTemplateCode;
    private String sign;
    private String eventTemplateCode;
    private String eventTemplate;

    public AliyunSmsSNSTextTemplateInventory() {
    }

    public AliyunSmsSNSTextTemplateInventory(AliyunSmsSNSTextTemplateVO vo) {
        setUuid(vo.getUuid());
        setName(vo.getName());
        setDescription(vo.getDescription());
        setApplicationPlatformType(vo.getApplicationPlatformType());
        setTemplate(vo.getTemplate());
        setRecoveryTemplate(vo.getRecoveryTemplate());
        setDefaultTemplate(vo.isDefaultTemplate());
        setCreateDate(vo.getCreateDate());
        setLastOpDate(vo.getLastOpDate());
        setAlarmTemplateCode(vo.getAlarmTemplateCode());
        setSign(vo.getSign());
        setEventTemplateCode(vo.getEventTemplateCode());
        setEventTemplate(vo.getEventTemplate());
        setType(vo.getType().toString());
    }

    public AliyunSmsSNSTextTemplateInventory(SNSTextTemplateInventory other) {
        super(other);
    }

    public static AliyunSmsSNSTextTemplateInventory __example__() {
        AliyunSmsSNSTextTemplateInventory inventory = new AliyunSmsSNSTextTemplateInventory(SNSTextTemplateInventory.__example__());
        inventory.alarmTemplateCode = "SMS_153055065";
        inventory.sign = "示例签名";
        inventory.eventTemplate = "Event ${EVENT_NAME} had just happened";
        inventory.eventTemplateCode = "SMS_123456789";
        inventory.setApplicationPlatformType("AliyunSms");
        return inventory;
    }

    public static AliyunSmsSNSTextTemplateInventory valueOf(AliyunSmsSNSTextTemplateVO vo) {
        return new AliyunSmsSNSTextTemplateInventory(vo);
    }

    public static List<AliyunSmsSNSTextTemplateInventory> valueOf1(Collection<AliyunSmsSNSTextTemplateVO> vos) {
        return vos.stream().map(AliyunSmsSNSTextTemplateInventory::valueOf).collect(Collectors.toList());
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

    public String getAlarmTemplateCode() {
        return alarmTemplateCode;
    }

    public void setAlarmTemplateCode(String alarmTemplateCode) {
        this.alarmTemplateCode = alarmTemplateCode;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}
