package org.zstack.header.storage.primary;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PrimaryStorageAO.class)
public class PrimaryStorageAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PrimaryStorageAO, String> zoneUuid;
    public static volatile SingularAttribute<PrimaryStorageAO, String> description;
    public static volatile SingularAttribute<PrimaryStorageAO, String> name;
    public static volatile SingularAttribute<PrimaryStorageAO, String> url;
    public static volatile SingularAttribute<PrimaryStorageAO, String> mountPath;
    public static volatile SingularAttribute<PrimaryStorageAO, String> type;
    public static volatile SingularAttribute<PrimaryStorageAO, PrimaryStorageState> state;
    public static volatile SingularAttribute<PrimaryStorageAO, PrimaryStorageStatus> status;
    public static volatile SingularAttribute<PrimaryStorageAO, Timestamp> createDate;
    public static volatile SingularAttribute<PrimaryStorageAO, Timestamp> lastOpDate;
}
