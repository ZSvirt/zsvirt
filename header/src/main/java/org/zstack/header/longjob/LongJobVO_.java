package org.zstack.header.longjob;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 11/13/17.
 */
@StaticMetamodel(LongJobVO.class)
public class LongJobVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<LongJobVO, String> name;
    public static volatile SingularAttribute<LongJobVO, String> description;
    public static volatile SingularAttribute<LongJobVO, String> apiId;
    public static volatile SingularAttribute<LongJobVO, String> jobName;
    public static volatile SingularAttribute<LongJobVO, String> jobData;
    public static volatile SingularAttribute<LongJobVO, String> jobResult;
    public static volatile SingularAttribute<LongJobVO, LongJobState> state;
    public static volatile SingularAttribute<LongJobVO, String> targetResourceUuid;
    public static volatile SingularAttribute<LongJobVO, String> managementNodeUuid;
    public static volatile SingularAttribute<LongJobVO, Timestamp> createDate;
    public static volatile SingularAttribute<LongJobVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<LongJobVO, String> parentUuid;
}
