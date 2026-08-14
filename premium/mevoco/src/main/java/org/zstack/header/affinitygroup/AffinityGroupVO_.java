package org.zstack.header.affinitygroup;

import org.zstack.header.vo.ResourceVO_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by shixin on 10/24/2017.
 */
@StaticMetamodel(AffinityGroupVO.class)
public class AffinityGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AffinityGroupVO, String> name;
    public static volatile SingularAttribute<AffinityGroupVO, String> description;
    public static volatile SingularAttribute<AffinityGroupVO, AffinityGroupPolicy> policy;
    public static volatile SingularAttribute<AffinityGroupVO, AffinityGroupType> type;
    public static volatile SingularAttribute<AffinityGroupVO, String> version;
    public static volatile SingularAttribute<AffinityGroupVO, String> appliance;
    public static volatile SingularAttribute<AffinityGroupVO, String> zoneUuid;
    public static volatile SingularAttribute<AffinityGroupVO, AffinityGroupState> state;
    public static volatile SingularAttribute<AffinityGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<AffinityGroupVO, Timestamp> lastOpDate;
}
