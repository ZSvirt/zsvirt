package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.query.AbstractMysqlQuerySubQueryExtension;
import org.zstack.query.QueryUtils;

/**
 */
public class AccountSubQueryExtension extends AbstractMysqlQuerySubQueryExtension {
    @Autowired
    private AccountManager acntMgr;

    @Override
    public String makeSubquery(APIQueryMessage msg, Class inventoryClass) {
        if (Account.isAllResourcesReadable(msg.getSession()))  {
            return null;
        }

        Class entityClass = QueryUtils.getEntityClassFromInventoryClass(inventoryClass);
        if (!acntMgr.isResourceHavingAccountReference(entityClass)) {
            return null;
        }

        String priKey = QueryUtils.getPrimaryKeyNameFromEntityClass(entityClass);

        String resourceType = acntMgr.getBaseResourceType(entityClass).getSimpleName();
        return String.format("(%s.%s in" +
                " (select distinct accountresourcerefvo.resourceUuid from AccountResourceRefVO accountresourcerefvo" +
                " where (accountresourcerefvo.accountUuid = '%s' or accountresourcerefvo.type = 'SharePublic')" +
                " and accountresourcerefvo.resourceType = '%s')",
        inventoryClass.getSimpleName().toLowerCase(), priKey, msg.getSession().getAccountUuid(), resourceType);
    }
}
