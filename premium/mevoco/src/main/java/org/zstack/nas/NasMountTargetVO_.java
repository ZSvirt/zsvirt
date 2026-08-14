package org.zstack.nas;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2018/3/8.
 */
@StaticMetamodel(NasMountTargetVO.class)
public class NasMountTargetVO_ extends ResourceVO_{
    public static volatile SingularAttribute<NasMountTargetVO, String> name;
    public static volatile SingularAttribute<NasMountTargetVO, String> nasFileSystemUuid;
    public static volatile SingularAttribute<NasMountTargetVO, String> description;
    public static volatile SingularAttribute<NasFileSystemVO, String> type;
    public static volatile SingularAttribute<NasFileSystemVO, String> mountDomain;
    public static volatile SingularAttribute<NasMountTargetVO, Timestamp> createDate;
    public static volatile SingularAttribute<NasMountTargetVO, Timestamp> lastOpDate;
}
