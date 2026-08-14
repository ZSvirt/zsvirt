package org.zstack.storage.ceph.primary;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/2/28.
 */
@StaticMetamodel(CephPrimaryStoragePoolVO.class)
public class CephPrimaryStoragePoolVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, String> poolName;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, String> aliasName;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, String> description;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, CephOsdGroupVO> osdGroup;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, Timestamp> createDate;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, String> type;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, Long> availableCapacity;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, Long> totalCapacity;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, Long> usedCapacity;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, Integer> replicatedSize;
}
