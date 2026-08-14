package org.zstack.sns.platform.dingtalk;

import org.zstack.sns.SNSApplicationEndpointVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SNSDingTalkEndpointVO.class)
public class SNSDingTalkEndpointVO_ extends SNSApplicationEndpointVO_ {
    public static volatile SingularAttribute<SNSDingTalkEndpointVO, String> url;
    public static volatile SingularAttribute<SNSDingTalkEndpointVO, Boolean> atAll;
    public static volatile SingularAttribute<SNSDingTalkEndpointVO, String> dingTalkSecret;
}
