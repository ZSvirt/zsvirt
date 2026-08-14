package org.zstack.identity.imports.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
@StaticMetamodel(ThirdPartyAccountSourceVO.class)
public class ThirdPartyAccountSourceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ThirdPartyAccountSourceVO, String> description;
    public static volatile SingularAttribute<ThirdPartyAccountSourceVO, String> type;
    public static volatile SingularAttribute<ThirdPartyAccountSourceVO, SyncCreatedAccountStrategy> createAccountStrategy;
    public static volatile SingularAttribute<ThirdPartyAccountSourceVO, SyncDeletedAccountStrategy> deleteAccountStrategy;
    public static volatile SingularAttribute<ThirdPartyAccountSourceVO, Timestamp> createDate;
    public static volatile SingularAttribute<ThirdPartyAccountSourceVO, Timestamp> lastOpDate;
}
