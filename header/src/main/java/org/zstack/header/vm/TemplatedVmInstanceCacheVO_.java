package org.zstack.header.vm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(TemplatedVmInstanceCacheVO.class)
public class TemplatedVmInstanceCacheVO_ {
    public static volatile SingularAttribute<TemplatedVmInstanceCacheVO, Long> id;
    public static volatile SingularAttribute<TemplatedVmInstanceCacheVO, String> templatedVmInstanceUuid;
    public static volatile SingularAttribute<TemplatedVmInstanceCacheVO, String> cacheVmInstanceUuid;
}
