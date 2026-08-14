package org.zstack.network.service.lb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-11
 **/
@StaticMetamodel(LoadBalancerListenerACLRefVO.class)
public class LoadBalancerListenerACLRefVO_ {
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, Long> id;
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, String> listenerUuid;
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, String> serverGroupUuid;
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, String> aclUuid;
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, LoadBalancerAclType> type;
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerListenerCertificateRefVO, Timestamp> lastOpDate;
}
