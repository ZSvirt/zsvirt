package org.zstack.testlib.identity.ldap

import org.zstack.core.db.Q
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorableValue
import org.zstack.ldap.LdapConstant
import org.zstack.ldap.driver.LdapSearchSpec
import org.zstack.ldap.driver.LdapUtil
import org.zstack.ldap.entity.LdapEntryInventory
import org.zstack.ldap.entity.LdapServerVO
import org.zstack.ldap.entity.LdapServerVO_

import java.util.function.Function
import java.util.function.Supplier

class LdapDriverSimulator extends LdapUtil {
    public Supplier<List<LdapVirtualEndpointSpec>> findAllEndpointsFunction
    public Function<LdapServerVO, LdapVirtualEndpointSpec> findEndpointByLdapVOFunction

    @Override
    ErrorCode testLdapServerConnection(LdapServerVO ldap) {
        def endpoint = findEndpointByLdapVOFunction.apply(ldap)
        if (endpoint != null) {
            return endpoint.testConnectionHandler.get()
        }
        return super.testLdapServerConnection(ldap)
    }

    @Override
    List<LdapEntryInventory> searchLdapEntry(LdapSearchSpec spec) {
        def ldap = Q.New(LdapServerVO.class)
                .eq(LdapServerVO_.uuid, spec.getLdapServerUuid())
                .find() as LdapServerVO
        def endpoint = findEndpointByLdapVOFunction.apply(ldap)
        if (endpoint != null) {
            return endpoint.searchHandler.apply(spec)
        }
        return super.searchLdapEntry(spec)
    }

    @Override
    boolean isValid(String uid, String password, LdapServerVO ldap) {
        def endpoint = findEndpointByLdapVOFunction.apply(ldap)
        if (endpoint != null) {
            return endpoint.authenticateHandler.apply(uid, password)
        }
        return super.isValid(uid, password, ldap)
    }

    @Override
    String getFullUserDn(LdapServerVO ldap, String key, String val) {
        def endpoint = findEndpointByLdapVOFunction.apply(ldap)
        if (endpoint == null) {
            return super.getFullUserDn(ldap, key, val)
        }

        LdapSearchSpec spec = new LdapSearchSpec()
        spec.ldapServerUuid = ldap.uuid
        spec.filter = LdapConstant.DEFAULT_PERSON_FILTER
        spec.returningAttributes = ["entryDN"]
        def list = endpoint.searchHandler.apply(spec)
        def matchedEntry = list.find { entry ->
            entry.attributes.any { attribute -> attribute.id == key && attribute.values.contains(val) }
        }

        return matchedEntry?.dn
    }

    @Override
    protected ErrorableValue<LdapEntryInventory> findLdapEntryByDn(String fullDn, LdapServerVO ldap) {
        def endpoint = findEndpointByLdapVOFunction.apply(ldap)
        if (endpoint == null) {
            return super.findLdapEntryByDn(fullDn, ldap)
        }

        LdapSearchSpec spec = new LdapSearchSpec()
        spec.ldapServerUuid = ldap.uuid
        spec.filter = LdapConstant.DEFAULT_PERSON_FILTER
        spec.returningAttributes = ["entryDN"]
        def list = endpoint.searchHandler.apply(spec)
        return ErrorableValue.of(list.find { it -> it.dn == fullDn })
    }
}
