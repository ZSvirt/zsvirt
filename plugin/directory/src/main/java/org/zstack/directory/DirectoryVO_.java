package org.zstack.directory;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * @author shenjin
 * @date 2022/11/29 10:48
 */
@StaticMetamodel(DirectoryVO.class)
public class DirectoryVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<DirectoryVO, String> name;
    public static volatile SingularAttribute<DirectoryVO, String> groupName;
    public static volatile SingularAttribute<DirectoryVO, String> parentUuid;
    public static volatile SingularAttribute<DirectoryVO, String> rootDirectoryUuid;
    public static volatile SingularAttribute<DirectoryVO, String> zoneUuid;
    public static volatile SingularAttribute<DirectoryVO, String> type;
    public static volatile SingularAttribute<DirectoryVO, Timestamp> createDate;
    public static volatile SingularAttribute<DirectoryVO, Timestamp> lastOpDate;
}
