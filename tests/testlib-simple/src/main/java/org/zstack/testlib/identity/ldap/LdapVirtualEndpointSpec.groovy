package org.zstack.testlib.identity.ldap

import org.zstack.header.errorcode.ErrorCode
import org.zstack.ldap.driver.LdapSearchSpec
import org.zstack.ldap.driver.LdapUtil
import org.zstack.ldap.entity.LdapEntryInventory
import org.zstack.ldap.entity.LdapServerVO
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Spec
import org.zstack.testlib.SpecID
import org.zstack.testlib.SpecParam

import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier

class LdapVirtualEndpointSpec extends Spec {
    @SpecParam(required = true)
    String name
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

    public String endpointUuid

    Supplier<ErrorCode> defaultTestConnectionHandler = { null }
    Supplier<ErrorCode> testConnectionHandler = defaultTestConnectionHandler
    Function<LdapSearchSpec, List<LdapEntryInventory>> defaultSearchHandler = { spec -> return [] }
    Function<LdapSearchSpec, List<LdapEntryInventory>> searchHandler = defaultSearchHandler
    BiFunction<String, String, Boolean> defaultAuthenticateHandler = { username, password -> true }
    BiFunction<String, String, Boolean> authenticateHandler = defaultAuthenticateHandler

    LdapVirtualEndpointSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    LdapServerSpec ldapServer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = LdapServerSpec.class) Closure c) {
        def i = new LdapServerSpec(envSpec)
        i.url = this.url
        i.base = this.base
        i.username = this.username
        i.password = this.password
        i.encryption = this.encryption
        c.delegate = i
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(i)
        return i
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        endpointUuid = uuid

        envSpec.mockFactory(LdapUtil.class) {
            def driver = new LdapDriverSimulator()
            driver.findAllEndpointsFunction = { findAllEndpoints() }
            driver.findEndpointByLdapVOFunction = { LdapServerVO ldap -> findEndpointByLdapVO(ldap) }
            return driver
        }

        return id(name, uuid)
    }

    List<LdapVirtualEndpointSpec> findAllEndpoints() {
        def endpointEntries = envSpec.specsByName.findAll { entry -> entry.value instanceof LdapVirtualEndpointSpec }
        return new ArrayList(endpointEntries.values()) as List<LdapVirtualEndpointSpec>
    }

    LdapVirtualEndpointSpec findEndpointByLdapVO(LdapServerVO ldap) {
        def specs = findAllEndpoints()
        def filtered = specs.findAll { it ->
            it.url == ldap.url &&
            it.base == ldap.base &&
            it.username == ldap.username &&
            it.password == ldap.password &&
            it.encryption == ldap.encryption
        }
        return filtered.isEmpty() ? null : filtered[0]
    }

    @Override
    void delete(String sessionId) {
        // do-nothing
    }

    LdapEntryInventoryBuilder ldapEntry() {
        return new LdapEntryInventoryBuilder()
    }

    void installTestConnectionHandler(Supplier<ErrorCode> handler) {
        this.testConnectionHandler = handler
    }

    void clearTestConnectionHandler() {
        this.testConnectionHandler = defaultTestConnectionHandler
    }

    void installSearchHandler(Function<LdapSearchSpec, List<LdapEntryInventory>> handler) {
        this.searchHandler = handler
    }

    void clearSearchHandler() {
        this.searchHandler = defaultSearchHandler
    }

    void installAuthenticateHandler(BiFunction<String, String, Boolean> handler) {
        this.authenticateHandler = handler
    }

    void clearAuthenticateHandler() {
        this.authenticateHandler = defaultAuthenticateHandler
    }
}
