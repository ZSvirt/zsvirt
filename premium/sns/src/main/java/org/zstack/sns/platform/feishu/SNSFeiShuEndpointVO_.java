package org.zstack.sns.platform.feishu;

import org.zstack.sns.SNSApplicationEndpointVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SNSFeiShuEndpointVO.class)
public class SNSFeiShuEndpointVO_ extends SNSApplicationEndpointVO_ {
    public static volatile SingularAttribute<SNSFeiShuEndpointVO, String> url;
    public static volatile SingularAttribute<SNSFeiShuEndpointVO, Boolean> atAll;
    public static volatile SingularAttribute<SNSFeiShuEndpointVO, String> feiShuSecret;
}
