package org.zstack.header.storage.database.backup;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(DatabaseBackupVO.class)
public class DatabaseBackupVO_ extends ResourceVO_{
    public static volatile SingularAttribute<DatabaseBackupVO, String> name;
    public static volatile SingularAttribute<DatabaseBackupVO, String> description;
    public static volatile SingularAttribute<DatabaseBackupVO, Long> size;
    public static volatile SingularAttribute<DatabaseBackupVO, DatabaseBackupState> state;
    public static volatile SingularAttribute<DatabaseBackupVO, DatabaseBackupStatus> status;
    public static volatile SingularAttribute<DatabaseBackupVO, String> metadata;
    public static volatile SingularAttribute<DatabaseBackupVO, Timestamp> createDate;
    public static volatile SingularAttribute<DatabaseBackupVO, Timestamp> lastOpDate;
}
