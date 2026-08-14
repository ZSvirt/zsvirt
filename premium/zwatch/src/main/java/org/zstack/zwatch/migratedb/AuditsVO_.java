package org.zstack.zwatch.migratedb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(AuditsVO.class)
public class AuditsVO_ {
    public static volatile SingularAttribute<AuditsVO, Long> id;
    public static volatile SingularAttribute<AuditsVO, Long> createTime;
    public static volatile SingularAttribute<AuditsVO, String> apiName;
    public static volatile SingularAttribute<AuditsVO, String> clientBrowser;
    public static volatile SingularAttribute<AuditsVO, String> clientIp;
    public static volatile SingularAttribute<AuditsVO, String> duration;
    public static volatile SingularAttribute<AuditsVO, String> error;
    public static volatile SingularAttribute<AuditsVO, String> operator;
    public static volatile SingularAttribute<AuditsVO, String> requestDump;
    public static volatile SingularAttribute<AuditsVO, String> resourceType;
    public static volatile SingularAttribute<AuditsVO, String> resourceUuid;
    public static volatile SingularAttribute<AuditsVO, String> requestUuid;
    public static volatile SingularAttribute<AuditsVO, String> responseDump;
    public static volatile SingularAttribute<AuditsVO, Boolean> success;
    public static volatile SingularAttribute<AuditsVO, String> signedText;
}
