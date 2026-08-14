package org.zstack.core.jsonlabel;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2016/9/13.
 */
@StaticMetamodel(JsonLabelVO.class)
public class JsonLabelVO_ {
    public static volatile SingularAttribute<Long, String> id;
    public static volatile SingularAttribute<Long, String> labelKey;
    public static volatile SingularAttribute<Long, String> labelValue;
    public static volatile SingularAttribute<Long, String> resourceUuid;
    public static volatile SingularAttribute<Long, Timestamp> createDate;
    public static volatile SingularAttribute<Long, Timestamp> lastOpDate;
}
