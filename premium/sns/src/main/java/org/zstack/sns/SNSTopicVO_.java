package org.zstack.sns;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SNSTopicVO.class)
public class SNSTopicVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SNSTopicVO, String> name;
    public static volatile SingularAttribute<SNSTopicVO, String> description;
    public static volatile SingularAttribute<SNSTopicVO, SNSTopicState> state;
    public static volatile SingularAttribute<SNSTopicVO, SNSTopicOwnerType> ownerType;
    public static volatile SingularAttribute<SNSTopicVO, String> locale;
    public static volatile SingularAttribute<SNSTopicVO, Timestamp> createDate;
    public static volatile SingularAttribute<SNSTopicVO, Timestamp> lastOpDate;
}
