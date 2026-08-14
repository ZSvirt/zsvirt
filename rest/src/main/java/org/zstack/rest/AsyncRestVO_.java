package org.zstack.rest;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2016/12/8.
 */
@StaticMetamodel(AsyncRestVO.class)
public class AsyncRestVO_ {
    public static volatile SingularAttribute<AsyncRestVO, String> uuid;
    public static volatile SingularAttribute<AsyncRestVO, String> requestData;
    public static volatile SingularAttribute<AsyncRestVO, AsyncRestState> state;
    public static volatile SingularAttribute<AsyncRestVO, String> result;
    public static volatile SingularAttribute<AsyncRestVO, Timestamp> createDate;
    public static volatile SingularAttribute<AsyncRestVO, Timestamp> lastOpDate;
}
