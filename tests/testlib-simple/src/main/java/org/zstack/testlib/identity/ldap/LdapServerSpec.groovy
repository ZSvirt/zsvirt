package org.zstack.testlib.identity.ldap

import org.zstack.sdk.identity.ldap.entity.LdapServerInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Spec
import org.zstack.testlib.SpecID
import org.zstack.testlib.SpecParam

class LdapServerSpec extends Spec {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    @SpecParam(required = true)
    String url
    @SpecParam(required = true)
    String base
    @SpecParam(required = true)
    String username
    @SpecParam(required = true)
    String password
    @SpecParam(required = true)
    String encryption
    @SpecParam
    String serverType = "Unknown"
    @SpecParam
    String usernameProperty = "cn"
    @SpecParam
    String filter
    @SpecParam
    String syncCreatedAccountStrategy = "CreateAccount"
    @SpecParam
    String syncDeletedAccountStrategy = "StaleAccount"

    LdapServerInventory inventory

    LdapServerSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        inventory = addLdapServer {
            delegate.name = name
            delegate.description = description
            delegate.url = url
            delegate.base = base
            delegate.username = username
            delegate.password = password
            delegate.encryption = encryption
            delegate.serverType = serverType
            delegate.usernameProperty = usernameProperty
            delegate.filter = filter
            delegate.syncCreatedAccountStrategy = syncCreatedAccountStrategy
            delegate.syncDeletedAccountStrategy = syncDeletedAccountStrategy
            delegate.sessionId = sessionId
        } as LdapServerInventory

        return id(inventory.name, inventory.uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteLdapServer {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }
        }
    }
}
