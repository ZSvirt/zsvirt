package org.zstack.kvm.hypervisor.datatype;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Wenhao.Zhang on 23/02/23
 */
@StaticMetamodel(KvmHostHypervisorMetadataVO.class)
public class KvmHostHypervisorMetadataVO_ {
    public static volatile SingularAttribute<KvmHostHypervisorMetadataVO, String> uuid;
    public static volatile SingularAttribute<KvmHostHypervisorMetadataVO, String> categoryUuid;
    public static volatile SingularAttribute<KvmHostHypervisorMetadataVO, String> managementNodeUuid;
    public static volatile SingularAttribute<KvmHostHypervisorMetadataVO, String> hypervisor;
    public static volatile SingularAttribute<KvmHostHypervisorMetadataVO, String> version;
    public static volatile SingularAttribute<KvmHostHypervisorMetadataVO, Timestamp> createDate;
    public static volatile SingularAttribute<KvmHostHypervisorMetadataVO, Timestamp> lastOpDate;
}
