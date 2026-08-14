package org.zstack.sso.header;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * @Author: DaoDao
 * @Date: 2023/2/3
 */
@StaticMetamodel(SSOTokenVO.class)
public class SSOTokenVO_ {
    public static volatile SingularAttribute<SSOTokenVO, String> uuid;
    public static volatile SingularAttribute<SSOTokenVO, String> clientUuid;
    public static volatile SingularAttribute<SSOTokenVO, String> userUuid;
    public static volatile SingularAttribute<SSOTokenVO, Timestamp> createDate;
    public static volatile SingularAttribute<SSOTokenVO, Timestamp> lastOpDate;
}
