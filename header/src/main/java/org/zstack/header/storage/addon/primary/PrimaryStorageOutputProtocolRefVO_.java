package org.zstack.header.storage.addon.primary;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(PrimaryStorageOutputProtocolRefVO.class)
public class PrimaryStorageOutputProtocolRefVO_ {
    public static volatile SingularAttribute<PrimaryStorageOutputProtocolRefVO, Long> id;
    public static volatile SingularAttribute<PrimaryStorageOutputProtocolRefVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<PrimaryStorageOutputProtocolRefVO, String> outputProtocol;
}
