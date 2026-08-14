package org.zstack.header.baremetal.pxeserver;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by GuoYi on 2018-10-10.
 */
@StaticMetamodel(BaremetalPxeServerClusterRefVO.class)
public class BaremetalPxeServerClusterRefVO_ {
    public static volatile SingularAttribute<BaremetalPxeServerClusterRefVO, Long> id;
    public static volatile SingularAttribute<BaremetalPxeServerClusterRefVO, String> clusterUuid;
    public static volatile SingularAttribute<BaremetalPxeServerClusterRefVO, String> pxeServerUuid;
    public static volatile SingularAttribute<BaremetalPxeServerClusterRefVO, String> createDate;
    public static volatile SingularAttribute<BaremetalPxeServerClusterRefVO, String> lastOpDate;
}
