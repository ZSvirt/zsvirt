package org.zstack.sns;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by Qi Le on 2019-07-10
 */
@StaticMetamodel(SNSSmsReceiverVO.class)
public class SNSSmsReceiverVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SNSSmsReceiverVO, String> endpointUuid;
    public static volatile SingularAttribute<SNSSmsReceiverVO, String> phoneNumber;
    public static volatile SingularAttribute<SNSSmsReceiverVO, SmsReceiverType> type;
    public static volatile SingularAttribute<SNSSmsReceiverVO, String> description;
}
