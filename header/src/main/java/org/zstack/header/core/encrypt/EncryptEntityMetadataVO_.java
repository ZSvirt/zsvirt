package org.zstack.header.core.encrypt;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(EncryptEntityMetadataVO.class)
public class EncryptEntityMetadataVO_ {
    public static volatile SingularAttribute<EncryptEntityMetadataVO_, Long> id;
    public static volatile SingularAttribute<EncryptEntityMetadataVO_, String> entityName;
    public static volatile SingularAttribute<EncryptEntityMetadataVO_, String> columnName;
    public static volatile SingularAttribute<EncryptEntityMetadataVO_, EncryptEntityState> state;
    public static volatile SingularAttribute<EncryptEntityMetadataVO_, Timestamp> createDate;
    public static volatile SingularAttribute<EncryptEntityMetadataVO_, Timestamp> lastOpDate;
}
