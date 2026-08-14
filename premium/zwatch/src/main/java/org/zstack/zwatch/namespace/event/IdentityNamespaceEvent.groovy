package org.zstack.zwatch.namespace.event

import org.zstack.header.identity.IdentityCanonicalEvents
import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.IdentityNamespace

class IdentityNamespaceEvent {
    IdentityNamespaceEvent() {
        IdentityNamespace.SessionForceLogout.onCanonicalEvent(IdentityCanonicalEvents.SESSION_FORCE_LOGOUT_PATH) { IdentityCanonicalEvents.SessionForceLogoutData data ->
            return new EventFamily.Event(data.sessionUuid, data.accountUuid, data.accountUuid)
        }
    }
}
