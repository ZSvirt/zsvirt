package org.zstack.test.integration.ldap

import com.unboundid.ldap.sdk.LDAPInterface
import org.junit.ClassRule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.ldap.LdapConstant
import org.zstack.sdk.*
import org.zstack.sdk.identity.ldap.api.AddLdapServerAction
import org.zstack.sdk.identity.ldap.entity.LdapServerInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.stabilisation.StabilityTestCase
import org.zstack.test.integration.stabilisation.TestCaseStabilityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017-11-09.
 */

class WindowsADCase extends SubCase {
    EnvSpec env

    public static String DOMAIN_DSN = "dc=example,dc=com"

    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build()

    String ldapUuid

    @Override
    void setup() {
        spring {
            include("accountImport.xml")
            include("LdapManagerImpl.xml")
            include("captcha.xml")
        }
    }

    @Override
    void environment() {
        env = env{
            zone {
                name = "zone"
                description = "test"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testAddLdapServer()
            testUpdateLdapServerType()
            testDeleteLdapServer()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    LDAPInterface getLdapConn(){
        try{
            return embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }

        try {
            return ZStackTest.embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }

        try {
            return StabilityTestCase.embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }

        try {
            return TestCaseStabilityTest.embeddedLdapRule.ldapConnection()
        }catch (Exception e){
        }
    }

    void testAddLdapServer(){
        LDAPInterface ldapConnection = this.getLdapConn()

        def configs = queryGlobalConfig {
            delegate.conditions = ["category=ldap", "name=current.ldap.server.uuid"]
        } as List<GlobalConfigInventory>
        assert configs.size() == 1
        assert configs[0].value == "NONE"

        def addLdapServerAction = new AddLdapServerAction(
                name : "ldap0",
                description : "test-ldap0",
                base : ZStackTest.DOMAIN_DSN,
                url : "ldap://localhost:1888",
                username : "",
                password : "",
                encryption : "None",
                serverType : "ErrorType",
        )
        expect(ApiException.class) {
            ZSClient.call(addLdapServerAction)
        }

        def result = addLdapServer {
            name = "ldap0"
            description = "test-ldap0"
            base = ZStackTest.DOMAIN_DSN
            url = "ldap://localhost:1888"
            username = ""
            password = ""
            encryption = "None"
            serverType = "WindowsAD"
        } as LdapServerInventory
        ldapUuid = result.uuid

        assert result.serverType == LdapConstant.WindowsAD.TYPE

        updateGlobalConfig {
            delegate.category = "ldap"
            delegate.name = "current.ldap.server.uuid"
            delegate.value = ldapUuid
        }
    }

    void testUpdateLdapServerType(){
        updateLdapServer {
            ldapServerUuid = ldapUuid
            serverType = "OpenLdap"
        }

        def ldapList = queryLdapServer {
            conditions = ["uuid=${ldapUuid}".toString()]
        } as List<LdapServerInventory>
        assert ldapList.size() == 1
        assert ldapList[0].serverType == LdapConstant.OpenLdap.TYPE
    }

    void testDeleteLdapServer(){
        def configs = queryGlobalConfig {
            delegate.conditions = ["category=ldap", "name=current.ldap.server.uuid"]
        } as List<GlobalConfigInventory>
        assert configs.size() == 1
        assert configs[0].value == ldapUuid

        deleteLdapServer {
            uuid = ldapUuid
        }

        configs = queryGlobalConfig {
            delegate.conditions = ["category=ldap", "name=current.ldap.server.uuid"]
        } as List<GlobalConfigInventory>
        assert configs.size() == 1
        assert configs[0].value == "NONE"

        // delete method can be call more than once
        deleteLdapServer {
            delegate.uuid = ldapUuid
        }

        deleteLdapServer {
            delegate.uuid = ldapUuid
        }
    }
}
