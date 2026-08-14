package org.zstack.header.aspect;

import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ResourceTypeMetadata;

import javax.persistence.EntityManager;

public class OwnedByAccountAspectHelper {
    public static void createAccountResourceRefVO(OwnedByAccount oa, EntityManager entityManager, Object entity) {
        AccountResourceRefVO ref = new AccountResourceRefVO();
        ref.setAccountUuid(oa.getAccountUuid());
        ref.setResourceType(ResourceTypeMetadata.getBaseResourceTypeFromConcreteType(entity.getClass()).getSimpleName());
        ref.setResourceUuid(OwnedByAccount.getResourceUuid(entity));
        ref.setType(AccessLevel.Own);

        entityManager.persist(ref);
    }
}
