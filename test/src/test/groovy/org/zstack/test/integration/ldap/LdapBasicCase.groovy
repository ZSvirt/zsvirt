package org.zstack.test.integration.ldap

import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchScope
import org.junit.ClassRule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.header.identity.IdentityErrors
import org.zstack.identity.IdentityGlobalConfig
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.identity.ldap.api.AddLdapServerAction
import org.zstack.sdk.identity.ldap.api.AddLdapServerResult
import org.zstack.sdk.ApiResult
import org.zstack.sdk.LogInAction
import org.zstack.sdk.ZSClient
import org.zstack.sdk.identity.ldap.entity.LdapEntryAttributeInventory
import org.zstack.sdk.identity.ldap.entity.LdapEntryInventory
import org.zstack.sdk.identity.ldap.entity.LdapServerInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.stabilisation.StabilityTestCase
import org.zstack.test.integration.stabilisation.TestCaseStabilityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

/**
 * Created by Administrator on 2017-03-22.
 */

//base on TestLdapConn
class LdapBasicCase extends SubCase {
    EnvSpec env

    public static String DOMAIN_DSN = "dc=example,dc=com"

    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build()

    String ldapUuid
    AccountInventory account1

    @Override
    void setup() {
        spring {
            kvm()
            localStorage()
            sftpBackupStorage()
            include("accountImport.xml")
            include("LdapManagerImpl.xml")
            include("captcha.xml")
        }
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testAddLdapServer()
            testRepeatToAddLdapServer()

            testGetLdapEntry()

            testGetCandidateLdapEntryForBinding()

            testCreateLdapBinding()

            testLoginByLdap()

            testDeleteLdapServer()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void prepare() {
        account1 = createAccount {
            delegate.name = "username1"
            delegate.password = "password"
        } as AccountInventory
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

    void testLoginByLdap(){

        String notExistCn = "nobody"
        String cn = "Micha Kops"
        String wrongPassword = "error"
        String rightPassword = "password"

        def action = new LogInAction(
                username: notExistCn,
                password: rightPassword,
                loginType: "ldap"
        )
        assert null != action.call().error

        action = new LogInAction(
                username: cn,
                password: wrongPassword,
                loginType: "ldap"
        )
        assert null != action.call().error

        IdentityGlobalConfig.MAX_CONCURRENT_SESSION.updateValue(1)

        logIn {
            username = cn
            password = rightPassword
            loginType = "ldap"
        }

        expectApiFailure ({
            logIn {
                delegate.username = cn
                delegate.password = rightPassword
                delegate.loginType = "ldap"
            }
        }) {
            assert code == "ID.1006"
            assert IdentityErrors.MAX_CONCURRENT_SESSION_EXCEEDED.toString() == "ID.1006"
        }

        IdentityGlobalConfig.MAX_CONCURRENT_SESSION.resetValue()

        logIn {
            username = cn
            password = rightPassword
            loginType = "ldap"
        }
    }

    void testGetLdapEntry(){

        def result = getLdapEntry {
            ldapFilter = "(cn=nobody)"
        } as List<LdapEntryInventory>
        assert 0 == result.size()

        result = getLdapEntry {
            ldapFilter = "(objectClass=person)"
        } as List<LdapEntryInventory>
        assert 3 == result.size()

        result = getLdapEntry {
            ldapFilter = "(objectClass=person)"
            limit = 1
        } as List<LdapEntryInventory>
        assert 1 == result.size()

        String cn = "Micha Kops"
        result = getLdapEntry {
            ldapFilter = "(cn=${cn})"
            limit = 2
        } as List<LdapEntryInventory>
        assert 1 == result.size()

        def attributes = result[0].attributes as List<LdapEntryAttributeInventory>
        def attribute = attributes.find { it.id == "cn" }
        assert attribute != null
        assert 1 == attribute.values.size()
        assert cn == attribute.values[0]

        result = getLdapEntry {
            ldapFilter = "(objectClass=person)"
            ldapServerUuid = ldapUuid
        } as List<LdapEntryInventory>
        assert 3 == result.size()
    }

    void testGetCandidateLdapEntryForBinding(){

        def result = getCandidateLdapEntryForBinding {
            ldapFilter = "(cn=nobody)"
        } as List<LdapEntryInventory>
        assert 0 == result.size()

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(objectClass=person)"
        } as List<LdapEntryInventory>
        assert 3 == result.size()

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(objectClass=person)"
            limit = 2
        } as List<LdapEntryInventory>
        assert 2 == result.size()

        String cn = "Micha Kops"
        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(cn=${cn})"
        } as List<LdapEntryInventory>
        assert 1 == result.size()
    }

    void testCreateLdapBinding(){
        String notExistCn = "nobody"
        expect (AssertionError.class) {
            createLdapBinding {
                accountUuid = account1.uuid
                ldapUid = notExistCn
            }
        }

        List result = getCandidateLdapEntryForBinding {
            ldapFilter = "(objectClass=person)"
        } as List<LdapEntryInventory>
        assert 3 == result.size()

        String dn = "cn=Micha Kops,ou=Users,dc=example,dc=com"
        createLdapBinding {
            accountUuid = account1.uuid
            ldapUid = dn
        }

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(objectClass=person)"
            limit = 10000
        } as List<LdapEntryInventory>
        assert 2 == result.size()

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(objectClass=person)"
        } as List<LdapEntryInventory>
        assert 2 == result.size()
        for(LdapEntryInventory map : result){
            assert dn != map.dn
        }

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(cn=Micha Kop)"
        } as List<LdapEntryInventory>
        assert 0 == result.size()

        result = getCandidateLdapEntryForBinding {
            ldapFilter = "(cn=Micha Kop)"
            limit = 1
        } as List<LdapEntryInventory>
        assert 0 == result.size()

        testGetLdapEntry()
    }

    void testAddLdapServer(){
        LDAPInterface ldapConnection = this.getLdapConn()
        final SearchResult searchResult = ldapConnection.search(ZStackTest.DOMAIN_DSN, SearchScope.SUB, "(objectClass=person)")
        assert searchResult.getEntryCount() == 3

        def result = addLdapServer {
            name = "ldap0"
            description = "test-ldap0"
            base = ZStackTest.DOMAIN_DSN
            url = "ldap://localhost:1888"
            username = ""
            password = ""
            encryption = "None"
            serverType = "OpenLdap"
            filter = "(!(cn=Micha Kops))"
        } as LdapServerInventory
        ldapUuid = result.uuid

        updateGlobalConfig {
            delegate.category = "ldap"
            delegate.name = "current.ldap.server.uuid"
            delegate.value = ldapUuid
        }

        AddLdapServerAction addLdapServerAction = new AddLdapServerAction(
                name : "ldap0",
                description : "test-ldap0",
                base : ZStackTest.DOMAIN_DSN,
                url : "ldap://localhost:1888",
                username : "",
                password : "",
                encryption : "None",
                systemTags : ["ephemeral::validationOnly"],
                sessionId : Test.currentEnvSpec.session.uuid
        )
        ApiResult res = ZSClient.call(addLdapServerAction)
        assert res.error == null
        assert null == res.getResult(AddLdapServerResult.class).inventory
    }

    void testRepeatToAddLdapServer(){

        AddLdapServerAction addLdapServerAction = new AddLdapServerAction(
                name : "ldap0",
                description : "test-ldap0",
                base : ZStackTest.DOMAIN_DSN,
                url : "ldap://localhost:1888",
                username : "",
                password : "",
                encryption : "None",
                sessionId : Test.currentEnvSpec.session.uuid
        )
        assert null != addLdapServerAction.call().error

        addLdapServerAction = new AddLdapServerAction(
                name : "ldap0",
                description : "test-ldap1",
                base : ZStackTest.DOMAIN_DSN,
                url : "ldap://172.20.11.200:1888",
                username : "uid=admin,cn=users,cn=accounts,dc=mevoco,dc=com",
                password : "password",
                encryption : "TLS",
                systemTags : ["ephemeral::validationOnly"],
                sessionId : Test.currentEnvSpec.session.uuid
        )
        assert null != addLdapServerAction.call().error

        addLdapServerAction = new AddLdapServerAction(
                name : "ldap0",
                description : "test-ldap1",
                base : ZStackTest.DOMAIN_DSN,
                url : "ldap://172.20.11.200:1888",
                username : "uid=admin,cn=users,cn=accounts,dc=mevoco,dc=com",
                password : "password",
                encryption : "TLS",
                sessionId : Test.currentEnvSpec.session.uuid
        )
        assert null != addLdapServerAction.call().error
    }

    void testDeleteLdapServer(){
        deleteLdapServer {
            uuid = ldapUuid
            sessionId = Test.currentEnvSpec.session.uuid
        }
    }
}
