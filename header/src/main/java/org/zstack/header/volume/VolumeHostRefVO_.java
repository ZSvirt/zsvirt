package org.zstack.header.volume;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VolumeHostRefVO.class)
public class VolumeHostRefVO_ {
    public static volatile SingularAttribute<VolumeHostRefVO, String> hostUuid;
    public static volatile SingularAttribute<VolumeHostRefVO, String> volumeUuid;
    public static volatile SingularAttribute<VolumeHostRefVO, String> mountPath;
    public static volatile SingularAttribute<VolumeHostRefVO, String> device;
    public static volatile SingularAttribute<VolumeHostRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeHostRefVO, Timestamp> lastOpDate;
}
