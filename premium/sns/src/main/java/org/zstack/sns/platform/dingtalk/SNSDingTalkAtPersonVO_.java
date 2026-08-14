package org.zstack.sns.platform.dingtalk;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SNSDingTalkAtPersonVO.class)
public class SNSDingTalkAtPersonVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SNSDingTalkAtPersonVO, String> phoneNumber;
    public static volatile SingularAttribute<SNSDingTalkAtPersonVO, String> endpointUuid;
}
