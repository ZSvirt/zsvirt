package org.zstack.directory;




import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * @author shenjin
 * @date 2022/11/29 11:36
 */
@StaticMetamodel(ResourceDirectoryRefVO.class)
public class ResourceDirectoryRefVO_ {
    public static volatile SingularAttribute<ResourceDirectoryRefVO, Long> id;
    public static volatile SingularAttribute<ResourceDirectoryRefVO, String> resourceUuid;
    public static volatile SingularAttribute<ResourceDirectoryRefVO, String> directoryUuid;
    public static volatile SingularAttribute<ResourceDirectoryRefVO, String> resourceType;
    public static volatile SingularAttribute<ResourceDirectoryRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<ResourceDirectoryRefVO, Timestamp> lastOpDate;
}
