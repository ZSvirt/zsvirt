package org.zstack.mevoco;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ShareableVolumeVmInstanceRefVO.class)
public class ShareableVolumeVmInstanceRefVO_ {
    public static volatile SingularAttribute<ShareableVolumeVmInstanceRefVO, String> uuid;
    public static volatile SingularAttribute<ShareableVolumeVmInstanceRefVO, String> volumeUuid;
    public static volatile SingularAttribute<ShareableVolumeVmInstanceRefVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<ShareableVolumeVmInstanceRefVO, Integer> deviceId;
    public static volatile SingularAttribute<ShareableVolumeVmInstanceRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<ShareableVolumeVmInstanceRefVO, Timestamp> lastOpDate;
}
