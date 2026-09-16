package org.zstack.identity;

import org.zstack.header.identity.SessionInventory;
import org.zstack.identity.rbac.AccessibleResourceChecker;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.zstack.utils.CollectionUtils.isEmpty;

public final class Resources {
    private Resources() {}

    public static boolean accessibleForAccount(String accountUuid, String resourceUuid) {
        if (accountUuid == null || resourceUuid == null) {
            return false;
        }
        return checker(accountUuid).isAccessible(resourceUuid);
    }

    public static boolean accessibleForSession(SessionInventory session, String resourceUuid) {
        if (session == null) {
            return false;
        }
        return accessibleForAccount(session.getAccountUuid(), resourceUuid);
    }

    public static List<String> filterAccessibleForAccount(String accountUuid, Collection<String> resourceUuids) {
        if (accountUuid == null || isEmpty(resourceUuids)) {
            return Collections.emptyList();
        }
        return checker(accountUuid).filterAccessible(resourceUuids);
    }

    public static List<String> filterAccessibleForSession(SessionInventory session, Collection<String> resourceUuids) {
        if (session == null) {
            return Collections.emptyList();
        }
        return filterAccessibleForAccount(session.getAccountUuid(), resourceUuids);
    }

    public static List<String> filterInaccessibleForAccount(String accountUuid, Collection<String> resourceUuids) {
        if (accountUuid == null || isEmpty(resourceUuids)) {
            return Collections.emptyList();
        }
        return checker(accountUuid).filterInaccessible(resourceUuids);
    }

    public static List<String> filterInaccessibleForSession(SessionInventory session, Collection<String> resourceUuids) {
        if (session == null) {
            return Collections.emptyList();
        }
        return filterInaccessibleForAccount(session.getAccountUuid(), resourceUuids);
    }

    private static AccessibleResourceChecker checker(String accountUuid) {
        return AccessibleResourceChecker.forAccount(accountUuid)
                .allowGlobalReadableResource()
                .asDetectedAdmin();
    }
}
