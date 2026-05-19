package org.zstack.identity;

import org.zstack.core.db.Q;
import org.zstack.header.identity.*;
import org.zstack.header.identity.role.RoleAccountRefVO;
import org.zstack.header.identity.role.RoleAccountRefVO_;

import java.util.Objects;

import static org.zstack.header.identity.AccountConstant.ALL_RESOURCES_READABLE_ROLE_UUID;
import static org.zstack.header.identity.AccountConstant.SOD_AUDITOR_ROLE_UUID;
import static org.zstack.header.identity.AccountConstant.SOD_SYSTEM_ADMIN_ROLE_UUID;

public interface Account {
    static String getAccountUuidOfResource(String resUuid) {
        return Q.New(AccountResourceRefVO.class)
                .select(AccountResourceRefVO_.accountUuid)
                .eq(AccountResourceRefVO_.resourceUuid, resUuid)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .findValue();
    }

    static boolean isAdminPermission(SessionInventory session) {
        return isAdminPermission(session.getAccountUuid());
    }

    static boolean isAdminPermission(String accountUuid) {
        if (isAdmin(accountUuid)) {
            return true;
        }

        return Q.New(AccountVO.class)
                .eq(AccountVO_.uuid, accountUuid)
                .eq(AccountVO_.type, AccountType.SystemAdmin)
                .isExists();
    }

    static boolean isAdminPermission(AccountInventory account) {
        if (account == null) {
            return false;
        }
        return isAdmin(account.getUuid()) || Objects.equals(account.getType(), AccountType.SystemAdmin.toString());
    }

    static boolean isAdmin(SessionInventory session) {
        return AccountConstant.isAdmin(session.getAccountUuid());
    }

    static boolean isAdmin(String accountUuid) {
        return AccountConstant.isAdmin(accountUuid);
    }

    static boolean isAllResourcesReadable(SessionInventory session) {
        return isAllResourcesReadable(session.getAccountUuid());
    }

    static boolean isAllResourcesReadable(String accountUuid) {
        if (isAdminPermission(accountUuid)) {
            return true;
        }

        return Q.New(RoleAccountRefVO.class)
                    .eq(RoleAccountRefVO_.accountUuid, accountUuid)
                    .eq(RoleAccountRefVO_.roleUuid, ALL_RESOURCES_READABLE_ROLE_UUID)
                    .isExists();
    }

    static boolean supportToQueryAuditsFromAllAccounts(SessionInventory session) {
        return supportToQueryAuditsFromAllAccounts(session.getAccountUuid());
    }

    static boolean supportToQueryAuditsFromAllAccounts(String accountUuid) {
        if (isAdmin(accountUuid)) {
            return true;
        }

        return Q.New(RoleAccountRefVO.class)
                    .eq(RoleAccountRefVO_.accountUuid, accountUuid)
                    .eq(RoleAccountRefVO_.roleUuid, SOD_AUDITOR_ROLE_UUID)
                    .isExists();
    }

    /**
     * include alert and events
     */
    static boolean supportToQueryEventsFromAllAccounts(SessionInventory session) {
        return supportToQueryEventsFromAllAccounts(session.getAccountUuid());
    }

    /**
     * include alert and events
     */
    static boolean supportToQueryEventsFromAllAccounts(String accountUuid) {
        if (isAdminPermission(accountUuid)) {
            return true;
        }

        return Q.New(RoleAccountRefVO.class)
                    .eq(RoleAccountRefVO_.accountUuid, accountUuid)
                    .eq(RoleAccountRefVO_.roleUuid, SOD_SYSTEM_ADMIN_ROLE_UUID)
                    .isExists();
    }
}
