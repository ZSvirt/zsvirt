package org.zstack.header.storageDevice;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Create by weiwang at 2018/8/2
 */
@StaticMetamodel(ScsiLunHostRefVO.class)
public class ScsiLunHostRefVO_ {
    public static volatile SingularAttribute<ScsiLunHostRefVO, Long> id;
    public static volatile SingularAttribute<ScsiLunHostRefVO, String> scsiLunUuid;
    public static volatile SingularAttribute<ScsiLunHostRefVO, String> hostUuid;
}
