package org.zstack.network.service.lb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 8/8/2015.
 */
@StaticMetamodel(LoadBalancerListenerCertificateRefVO.class)
public class LoadBalancerListenerCertificateRefVO_ {
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, Long> id;
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, String> listenerUuid;
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, String> certificateUuid;
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, Timestamp> lastOpDate;
}
