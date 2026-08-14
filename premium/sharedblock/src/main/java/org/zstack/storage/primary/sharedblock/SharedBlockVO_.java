package org.zstack.storage.primary.sharedblock;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SharedBlockVO.class)
public class SharedBlockVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SharedBlockVO, String> sharedBlockGroupUuid;
    public static volatile SingularAttribute<SharedBlockVO, String> type;
    public static volatile SingularAttribute<SharedBlockVO, String> diskUuid;
    public static volatile SingularAttribute<SharedBlockVO, String> name;
    public static volatile SingularAttribute<SharedBlockVO, String> description;
    public static volatile SingularAttribute<SharedBlockVO, SharedBlockState> state;
    public static volatile SingularAttribute<SharedBlockVO, SharedBlockStatus> status;
    public static volatile SingularAttribute<SharedBlockVO, Timestamp> createDate;
    public static volatile SingularAttribute<SharedBlockVO, Timestamp> lastOpDate;
}
