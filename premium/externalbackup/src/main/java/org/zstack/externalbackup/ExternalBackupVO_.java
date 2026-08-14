package org.zstack.externalbackup;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by MaJin on 2019/11/30.
 */
@StaticMetamodel(ExternalBackupVO.class)
public class ExternalBackupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ExternalBackupVO, String> name;
    public static volatile SingularAttribute<ExternalBackupVO, String> description;
    public static volatile SingularAttribute<ExternalBackupVO, ExternalBackupState> state;
    public static volatile SingularAttribute<ExternalBackupVO, String> installPath;
    public static volatile SingularAttribute<ExternalBackupVO, Long> totalSize;
    public static volatile SingularAttribute<ExternalBackupVO, String> version;
    public static volatile SingularAttribute<ExternalBackupVO, Timestamp> createDate;
    public static volatile SingularAttribute<ExternalBackupVO, Timestamp> lastOpDate;
}
