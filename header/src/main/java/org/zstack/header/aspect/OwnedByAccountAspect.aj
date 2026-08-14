package org.zstack.header.aspect;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ResourceVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.EntityManager;
import java.util.function.Function;

public aspect OwnedByAccountAspect {
    private static final CLogger logger = Utils.getLogger(OwnedByAccountAspect.class);

    private static Function<String, String> accountUuidGetter;

    public static Function<String, String> getAccountUuidGetter() {
        return accountUuidGetter;
    }

    public static void setAccountUuidGetter(Function<String, String> accountUuidGetter) {
        OwnedByAccountAspect.accountUuidGetter = accountUuidGetter;
    }

    Object around(OwnedByAccount oa) : this(oa) && execution(java.lang.String OwnedByAccount+.getAccountUuid()) {
        Object accountUuid = proceed(oa);

        if (accountUuid == null) {
            accountUuid = accountUuidGetter.apply(((ResourceVO) oa).getUuid());
        }

        return accountUuid;
    }

    after(EntityManager mgr, Object entity) : call(void EntityManager+.persist(Object))
            && target(mgr)
            && args(entity) {
        if (!(entity instanceof OwnedByAccount)) {
            return;
        }

        OwnedByAccount oa = (OwnedByAccount) entity;
        OwnedByAccountAspectHelper.createAccountResourceRefVO(oa, mgr, entity);
    }
}