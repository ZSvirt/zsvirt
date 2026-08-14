package org.zstack.sns.platform.email;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SNSEmailAddressVO.class)
public class SNSEmailAddressVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SNSEmailAddressVO, String> emailAddress;
    public static volatile SingularAttribute<SNSEmailAddressVO, String> endpointUuid;
}
