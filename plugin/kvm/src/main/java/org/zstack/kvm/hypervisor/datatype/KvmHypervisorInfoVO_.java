package org.zstack.kvm.hypervisor.datatype;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Wenhao.Zhang on 23/02/23
 */
@StaticMetamodel(KvmHypervisorInfoVO.class)
public class KvmHypervisorInfoVO_ {
    public static volatile SingularAttribute<KvmHypervisorInfoVO, String> uuid;
    public static volatile SingularAttribute<KvmHypervisorInfoVO, String> hypervisor;
    public static volatile SingularAttribute<KvmHypervisorInfoVO, String> version;
    public static volatile SingularAttribute<KvmHypervisorInfoVO, HypervisorVersionState> matchState;
    public static volatile SingularAttribute<KvmHypervisorInfoVO, Timestamp> createDate;
    public static volatile SingularAttribute<KvmHypervisorInfoVO, Timestamp> lastOpDate;
}
