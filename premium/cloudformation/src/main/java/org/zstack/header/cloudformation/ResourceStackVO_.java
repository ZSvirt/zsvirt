package org.zstack.header.cloudformation;

import org.zstack.cloudformation.ResourceStackStatus;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
@StaticMetamodel(ResourceStackVO.class)
public class ResourceStackVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ResourceStackVO, String> accountUuid;
    public static volatile SingularAttribute<ResourceStackVO, String> uuid;
    public static volatile SingularAttribute<ResourceStackVO, String> name;
    public static volatile SingularAttribute<ResourceStackVO, String> description;
    public static volatile SingularAttribute<ResourceStackVO, String> type;
    public static volatile SingularAttribute<ResourceStackVO, String> version;
    public static volatile SingularAttribute<ResourceStackVO, String> templateContent;
    public static volatile SingularAttribute<ResourceStackVO, String> paramContent;
    public static volatile SingularAttribute<ResourceStackVO, String> zoneUuid;
    public static volatile SingularAttribute<ResourceStackVO, String> reason;
    public static volatile SingularAttribute<ResourceStackVO, String> outputs;
    public static volatile SingularAttribute<ResourceStackVO, Boolean> enableRollback;
    public static volatile SingularAttribute<ResourceStackVO, ResourceStackStatus> status;
    public static volatile SingularAttribute<ResourceStackVO, Timestamp> createDate;
    public static volatile SingularAttribute<ResourceStackVO, Timestamp> lastOpDate;
}
