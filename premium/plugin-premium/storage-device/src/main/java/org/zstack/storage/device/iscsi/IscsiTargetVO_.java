package org.zstack.storage.device.iscsi;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/2
 */
@StaticMetamodel(IscsiTargetVO.class)
public class IscsiTargetVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<IscsiTargetVO, String> iqn;
    public static volatile SingularAttribute<IscsiTargetVO, String> clusterUuid;
    public static volatile SingularAttribute<IscsiTargetVO, String> iscsiServerUuid;
    public static volatile SingularAttribute<IscsiTargetVO, Timestamp> createDate;
    public static volatile SingularAttribute<IscsiTargetVO, Timestamp> lastOpDate;
}
