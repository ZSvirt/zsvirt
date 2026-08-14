package org.zstack.test.integration.ldap

import org.junit.ClassRule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.ldap.LdapConstant
import org.zstack.sdk.identity.ldap.entity.LdapServerInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * @author shenjin
 * @date 2022/10/24 16:45
 */
class LdapFilterCase extends SubCase {
    EnvSpec env

    LdapServerInventory ldapServer

    public static String DOMAIN_DSN = "dc=example,dc=com"
    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance()
            .bindingToPort(1888)
            .usingDomainDsn(DOMAIN_DSN)
            .importingLdifs("users-import.ldif")
            .build()

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testUpdateFiler()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void prepare() {
        ldapServer = addLdapServer {
            name = "ldap0"
            description = "test-ldap0"
            base = DOMAIN_DSN
            url = "ldap://localhost:1888"
            username = ""
            password = ""
            encryption = "None"
        } as LdapServerInventory

        assert ldapServer.filter == LdapConstant.DEFAULT_PERSON_FILTER
    }

    void testUpdateFiler(){
        String filter = "(age=3)"

        updateLdapServer {
            ldapServerUuid = ldapServer.uuid
            delegate.filter = filter
        }

        def ldapList = queryLdapServer {
            delegate.conditions = ["uuid=${ldapServer.uuid}"]
        } as List<LdapServerInventory>
        assert ldapList.size() == 1
        assert ldapList[0].filter == filter
    }
}
