package org.zstack.storage.device.iscsi;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Create by weiwang at 2018/8/2
 */
@StaticMetamodel(IscsiServerClusterRefVO.class)
public class IscsiServerClusterRefVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<IscsiLunVO, String> iscsiServerUuid;
    public static volatile SingularAttribute<IscsiLunVO, String> clusterUuid;
}
