package org.zstack.nas;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2018/3/5.
 */
@StaticMetamodel(NasFileSystemVO.class)
public class NasFileSystemVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<NasFileSystemVO, NasProtocolType> protocol;
    public static volatile SingularAttribute<NasFileSystemVO, String> fileSystemId;
    public static volatile SingularAttribute<NasFileSystemVO, String> type;
    public static volatile SingularAttribute<NasFileSystemVO, String> name;
    public static volatile SingularAttribute<NasFileSystemVO, String> description;
    public static volatile SingularAttribute<NasFileSystemVO, Timestamp> createDate;
    public static volatile SingularAttribute<NasFileSystemVO, Timestamp> lastOpDate;
}
