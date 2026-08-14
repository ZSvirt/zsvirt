package org.zstack.loginControl.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by kayo on 2018/8/6.
 */
@StaticMetamodel(LoginAttemptsVO.class)
public class LoginAttemptsVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<LoginAttemptsVO, String> targetResourceIdentity;
    public static volatile SingularAttribute<LoginAttemptsVO, Integer> attempts;
    public static volatile SingularAttribute<LoginAttemptsVO, Boolean> forceChangePassword;
    public static volatile SingularAttribute<LoginAttemptsVO, Boolean> locked;
    public static volatile SingularAttribute<LoginAttemptsVO, Timestamp> unlockDate;
    public static volatile SingularAttribute<LoginAttemptsVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoginAttemptsVO, Timestamp> lastOpDate;
}
