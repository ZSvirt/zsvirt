package org.zstack.header.message;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by MaJin on 2020/10/20.
 */
@StaticMetamodel(ReplayMessageVO.class)
public class ReplayMessageVO_ {
    public static volatile SingularAttribute<ReplayMessageVO, Long> id;
    public static volatile SingularAttribute<ReplayMessageVO, String> msgDump;
    public static volatile SingularAttribute<ReplayMessageVO, String> locationType;
    public static volatile SingularAttribute<ReplayMessageVO, String> locationUuid;
    public static volatile SingularAttribute<ReplayMessageVO, String> resourceUuid;
    public static volatile SingularAttribute<ReplayMessageVO, String> groupUuid;
    public static volatile SingularAttribute<ReplayMessageVO, String> manageJobUuid;
    public static volatile SingularAttribute<ReplayMessageVO, Timestamp> createDate;
    public static volatile SingularAttribute<ReplayMessageVO, Timestamp> lastOpDate;
}