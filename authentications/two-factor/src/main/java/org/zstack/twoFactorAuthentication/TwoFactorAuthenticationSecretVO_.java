package org.zstack.twoFactorAuthentication;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by shixin on 06/28/2018.
 */
@StaticMetamodel(TwoFactorAuthenticationSecretVO.class)
public class TwoFactorAuthenticationSecretVO_ {
    public static volatile SingularAttribute<TwoFactorAuthenticationSecretVO, String> uuid;
    public static volatile SingularAttribute<TwoFactorAuthenticationSecretVO, String> secret;
    public static volatile SingularAttribute<TwoFactorAuthenticationSecretVO, String> accountUuid;
    public static volatile SingularAttribute<TwoFactorAuthenticationSecretVO, TwoFactorAuthenticationSecretStatus> status;
    public static volatile SingularAttribute<TwoFactorAuthenticationSecretVO, Timestamp> createDate;
    public static volatile SingularAttribute<TwoFactorAuthenticationSecretVO, Timestamp> lastOpDate;
}
