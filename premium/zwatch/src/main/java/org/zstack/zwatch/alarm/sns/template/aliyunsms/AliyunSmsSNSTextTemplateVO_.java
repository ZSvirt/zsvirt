package org.zstack.zwatch.alarm.sns.template.aliyunsms;

import org.zstack.zwatch.alarm.sns.SNSTextTemplateVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by Qi Le on 2019-07-12
 */
@StaticMetamodel(AliyunSmsSNSTextTemplateVO.class)
public class AliyunSmsSNSTextTemplateVO_ extends SNSTextTemplateVO_ {
    public static volatile SingularAttribute<AliyunSmsSNSTextTemplateVO, String> alarmTemplateCode;
    public static volatile SingularAttribute<AliyunSmsSNSTextTemplateVO, String> sign;
    public static volatile SingularAttribute<AliyunSmsSNSTextTemplateVO, String> eventTemplateCode;
    public static volatile SingularAttribute<AliyunSmsSNSTextTemplateVO, String> eventTemplate;
}
