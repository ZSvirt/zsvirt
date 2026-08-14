package org.zstack.sns.platform.feishu;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SNSFeiShuAtPersonVO.class)
public class SNSFeiShuAtPersonVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SNSFeiShuAtPersonVO, String> userId;
    public static volatile SingularAttribute<SNSFeiShuAtPersonVO, String> endpointUuid;
}
