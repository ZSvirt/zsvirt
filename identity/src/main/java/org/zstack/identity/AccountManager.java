package org.zstack.identity;

import org.zstack.header.identity.*;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.message.Message;

import java.util.List;
import java.util.Map;

public interface AccountManager {
    String getOwnerAccountUuidOfResource(String resourceUuid);

    boolean isResourceHavingAccountReference(Class entityClass);

    List<String> getResourceUuidsCanAccessByAccount(String accountUuid, Class resourceType);

    Map<Class, List<Quota>> getMessageQuotaMap();

    Map<Class, List<QuotaMessageHandler<? extends Message>>> getQuotaMessageHandlerMap();

    Map<String, QuotaDefinition> getQuotasDefinitions();

    AccountResourceRefInventory changeResourceOwner(String resourceUuid, String newOwnerUuid);

    /**
     * return true only when the account is "admin" account
     */
    boolean isAdmin(SessionInventory session);

    /**
     * return true if the account is in {@link AccountType#SystemAdmin} type
     */
    boolean isInSystemAdminType(SessionInventory session);

    void adminAdoptAllOrphanedResource(List<String> resourceUuid, String originAccountUuid);

    Class getBaseResourceType(Class clz);

    AccountInventory createAccount(CreateAccountMsg msg);
}
