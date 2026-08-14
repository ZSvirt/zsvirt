package org.zstack.identity.imports.entity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
@StaticMetamodel(AccountThirdPartyAccountSourceRefVO.class)
public class AccountThirdPartyAccountSourceRefVO_ {
    public static volatile SingularAttribute<AccountThirdPartyAccountSourceRefVO, Long> id;
    public static volatile SingularAttribute<AccountThirdPartyAccountSourceRefVO, String> credentials;
    public static volatile SingularAttribute<AccountThirdPartyAccountSourceRefVO, String> accountSourceUuid;
    public static volatile SingularAttribute<AccountThirdPartyAccountSourceRefVO, String> accountUuid;
    public static volatile SingularAttribute<AccountThirdPartyAccountSourceRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<AccountThirdPartyAccountSourceRefVO, Timestamp> lastOpDate;
}
