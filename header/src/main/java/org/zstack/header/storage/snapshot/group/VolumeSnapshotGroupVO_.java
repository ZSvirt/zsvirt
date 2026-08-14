package org.zstack.header.storage.snapshot.group;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by MaJin on 2019/7/10.
 */
@StaticMetamodel(VolumeSnapshotGroupVO.class)
public class VolumeSnapshotGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VolumeSnapshotGroupVO, String> name;
    public static volatile SingularAttribute<VolumeSnapshotGroupVO, String> description;
    public static volatile SingularAttribute<VolumeSnapshotGroupVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VolumeSnapshotGroupVO, Integer> snapshotCount;
    public static volatile SingularAttribute<VolumeSnapshotGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeSnapshotGroupVO, Timestamp> lastOpDate;
}
