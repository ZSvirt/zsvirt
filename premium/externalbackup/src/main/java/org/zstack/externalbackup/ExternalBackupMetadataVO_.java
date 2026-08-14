package org.zstack.externalbackup;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ExternalBackupMetadataVO.class)
public class ExternalBackupMetadataVO_ {
    public static volatile SingularAttribute<ExternalBackupMetadataVO, String> uuid;
    public static volatile SingularAttribute<ExternalBackupMetadataVO, String> metadata;
}
