package org.zstack.zwatch.alarm.sns;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SNSTextTemplateVO.class)
public class SNSTextTemplateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SNSTextTemplateVO, String> name;
    public static volatile SingularAttribute<SNSTextTemplateVO, String> description;
    public static volatile SingularAttribute<SNSTextTemplateVO, String> applicationPlatformType;
    public static volatile SingularAttribute<SNSTextTemplateVO, String> subject;
    public static volatile SingularAttribute<SNSTextTemplateVO, String> recoverySubject;
    public static volatile SingularAttribute<SNSTextTemplateVO, String> template;
    public static volatile SingularAttribute<SNSTextTemplateVO, String> recoveryTemplate;
    public static volatile SingularAttribute<SNSTextTemplateVO, Boolean> defaultTemplate;
    public static volatile SingularAttribute<SNSTextTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<SNSTextTemplateVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<SNSTextTemplateVO, SNSTextTemplateType> type;
}
