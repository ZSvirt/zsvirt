package org.zstack.header.host;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by LiangHanYu on 2021/8/25 14:10
 */
@StaticMetamodel(CpuFeaturesHistoryVO.class)
public class CpuFeaturesHistoryVO_ {
    public static volatile SingularAttribute<CpuFeaturesHistoryVO, Long> id;
    public static volatile SingularAttribute<CpuFeaturesHistoryVO, String> srcHostUuid;
    public static volatile SingularAttribute<CpuFeaturesHistoryVO, String> dstHostUuid;
    public static volatile SingularAttribute<CpuFeaturesHistoryVO, String> srcCpuModelName;
    public static volatile SingularAttribute<CpuFeaturesHistoryVO, Boolean> supportLiveMigration;
    public static volatile SingularAttribute<CpuFeaturesHistoryVO, Timestamp> createDate;
    public static volatile SingularAttribute<CpuFeaturesHistoryVO, Timestamp> lastOpDate;
}
