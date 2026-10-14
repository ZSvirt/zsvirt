package org.zstack.test.integration.identity.account

import org.zstack.core.db.Q
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.identity.AccountSource
import org.zstack.header.identity.AccountVO
import org.zstack.header.identity.AccountVO_
import org.zstack.identity.AccountPassword
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO_
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.LogInByAccountAction
import org.zstack.sdk.LogInAction
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.identity.imports.entity.AccountThirdPartyAccountSourceRefInventory
import org.zstack.sdk.identity.ldap.entity.LdapServerInventory
import org.zstack.test.integration.identity.IdentityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.identity.ldap.LdapVirtualEndpointSpec

import java.lang.reflect.Field

class EqualNameAccountCase extends SubCase {
    EnvSpec env

    LdapServerInventory ldap
    LdapVirtualEndpointSpec endpoint

    @Override
    void setup() {
        useSpring(IdentityTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "z"
            }

            ldapEndpoint {
                name = "ldapEndpoint0"
                base = "dc=EqualNameAccountCase,dc=zsv"
                url = "ldap://test.zstack.io:10001"
                username = "EqualNameAccountCase"
                password = "none"
                encryption = "None"

                ldapServer {
                    name = "ldap0"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testCreateCoexistAndDuplicate()
            testSchemeBShadowAndChangePassword()
            testNoLocalKeepsLegacyLocalLogin()
            testLoginEntryRejectsSourceAndUuid()
            testRegressZsv12290SameNameBinding()
        }
    }

    void prepare() {
        ldap = env.inventoryByName("ldap0") as LdapServerInventory
        endpoint = env.specByName("ldapEndpoint0") as LdapVirtualEndpointSpec

        updateGlobalConfig {
            delegate.category = "ldap"
            delegate.name = "current.ldap.server.uuid"
            delegate.value = ldap.uuid
        }
    }

    void testCreateCoexistAndDuplicate() {
        logger.info("Test 001: create a local account[zhangsan], expect success")
        def localZhangsan = createAccount {
            delegate.name = "zhangsan"
            delegate.password = "password"
        } as AccountInventory
        assert localZhangsan.source == AccountSource.Local.name()
        // verify the local account is persisted in DB with source=Local
        AccountVO localZhangsanVO = dbFindByUuid(localZhangsan.uuid, AccountVO.class)
        assert localZhangsanVO != null
        assert localZhangsanVO.source == AccountSource.Local

        def accounts001 = queryAccount {
            delegate.conditions = [
                "name=zhangsan", "source=" + AccountSource.Local.toString()
            ]
        } as List<AccountInventory>
        assert accounts001.size() == 1

        logger.info("Test 002: create the local account[zhangsan] again, expect API failure (duplicate within source Local)")
        expectApiFailure({
            createAccount {
                delegate.name = "zhangsan"
                delegate.password = "password"
            }
        }) {
            assert code == SysErrors.INVALID_ARGUMENT_ERROR.toString()
        }

        def accounts002 = queryAccount {
            delegate.conditions = [
                "name=zhangsan", "source=" + AccountSource.Local.toString()
            ]
        } as List<AccountInventory>
        assert accounts002.size() == 1

        logger.info("Test 003: import a same-name account[zhangsan] from LDAP (source=WindowsAD), expect success and coexist with the local one")
        installLdapUsers("zhangsan")
        syncAccountsFromLdapServer {
            delegate.uuid = ldap.uuid
        }

        def accounts003 = queryAccount {
            delegate.conditions = [
                "name=zhangsan", "source=" + AccountSource.WindowsAD.toString()
            ]
        } as List<AccountInventory>
        assert accounts003.size() == 1
        String ldapZhangsanUuid = Q.New(AccountVO.class).select(AccountVO_.uuid)
                .eq(AccountVO_.@name, "zhangsan")
                .eq(AccountVO_.source, AccountSource.WindowsAD)
                .findValue()
        assert Q.New(AccountThirdPartyAccountSourceRefVO.class)
                .eq(AccountThirdPartyAccountSourceRefVO_.accountUuid, ldapZhangsanUuid)
                .isExists()

        logger.info("Test 004: query account[zhangsan], expect two accounts with different sources (Local + WindowsAD) coexisting")
        def zhangsans = queryAccount {
            delegate.conditions = ["name=zhangsan"]
        } as List<AccountInventory>
        assert zhangsans.size() == 2
        assert zhangsans.collect { it.source }.toSet() == [AccountSource.Local.name(), AccountSource.WindowsAD.name()].toSet()
        // verify the same coexistence at DB level
        assert Q.New(AccountVO.class).eq(AccountVO_.@name, "zhangsan").count() == 2

        logger.info("Test 005: sync LDAP again, expect idempotent (still a single WindowsAD account[zhangsan], no duplicate within source)")
        syncAccountsFromLdapServer {
            delegate.uuid = ldap.uuid
        }
        def afterResync = queryAccount {
            delegate.conditions = ["name=zhangsan"]
        } as List<AccountInventory>
        assert afterResync.size() == 2

        def accounts005 = queryAccount {
            delegate.conditions = [
                "name=zhangsan", "source=" + AccountSource.WindowsAD.toString()
            ]
        } as List<AccountInventory>
        assert accounts005.size() == 1

        logger.info("Test 099: clean accounts named zhangsan")
        cleanupAccountsNamed("zhangsan")
        clearLdapUsers()

        assert !Q.New(AccountVO.class).eq(AccountVO_.@name, "zhangsan").isExists()
    }

    void testSchemeBShadowAndChangePassword() {
        def localZhangsan = createAccount {
            delegate.name = "zhangsan"
            delegate.password = "localpassword"
        } as AccountInventory

        installLdapUsers("zhangsan")
        syncAccountsFromLdapServer {
            delegate.uuid = ldap.uuid
        }

        def accounts100 = queryAccount {
            delegate.conditions = [
                "name=zhangsan"
            ]
        } as List<AccountInventory>
        assert accounts100.size() == 2
        assert accounts100.count { it.source == AccountSource.Local.toString() } == 1
        assert accounts100.count { it.source == AccountSource.WindowsAD.toString() } == 1
        def ldapZhangsan = accounts100.find {it.source == AccountSource.WindowsAD.toString() }

        logger.info("Test 101: local password login zhangsan, expect to hit the Local account (shadowing the WindowsAD one)")
        SessionInventory localSession = logInByAccount {
            delegate.accountName = "zhangsan"
            delegate.password = "localpassword"
        } as SessionInventory
        assert localSession.accountUuid == localZhangsan.uuid

        logger.info("Test 102: local password login zhangsan with a wrong password, expect login fail (AUTHENTICATION_ERROR)")
        expectApiFailure({
            logInByAccount {
                delegate.accountName = "zhangsan"
                delegate.password = "wrongpassword"
            }
        }) {
            assert code == "ID.1000"
        }

        logger.info("Test 103: LDAP login zhangsan (loginType=ldap), expect to hit the imported WindowsAD account via ref, not the Local one")
        endpoint.installAuthenticateHandler { username, password -> true }
        SessionInventory ldapSession = logIn {
            delegate.username = "zhangsan"
            delegate.password = "password"
            delegate.loginType = "ldap"
        } as SessionInventory
        assert ldapSession.accountUuid == ldapZhangsan.uuid

        logger.info("Test 105: update the password of the shadowed WindowsAD account[zhangsan], expect API failure (local password is meaningless under scheme B)")
        String ldapPasswordBefore = dbFindByUuid(ldapZhangsan.uuid, AccountVO.class).password
        expectApiFailure({
            updateAccount {
                delegate.uuid = ldapZhangsan.uuid
                delegate.password = "newpassword"
            }
        }) {
            assert code == SysErrors.OPERATION_ERROR.toString()
        }

        assert dbFindByUuid(ldapZhangsan.uuid, AccountVO.class).password == ldapPasswordBefore

        logger.info("Test 106: update the password of the Local account[zhangsan], expect success (Local is never shadowed)")
        updateAccount {
            delegate.uuid = localZhangsan.uuid
            delegate.password = "localpassword2"
        }

        String localStored = dbFindByUuid(localZhangsan.uuid, AccountVO.class).password
        assert AccountPassword.isEncoded(localStored)
        assert AccountPassword.matches("localpassword2", localStored)
        logInByAccount {
            delegate.accountName = "zhangsan"
            delegate.password = "localpassword2"
        }

        logger.info("Test 199: clean accounts named zhangsan")
        cleanupAccountsNamed("zhangsan")
        clearLdapUsers()

        assert !Q.New(AccountVO.class).eq(AccountVO_.@name, "zhangsan").isExists()
    }

    void testNoLocalKeepsLegacyLocalLogin() {
        installLdapUsers("lisi")
        syncAccountsFromLdapServer {
            delegate.uuid = ldap.uuid
        }

        def accounts300A = queryAccount {
            delegate.conditions = [
                "name=lisi", "source=" + AccountSource.WindowsAD.toString()
            ]
        } as List<AccountInventory>
        assert accounts300A.size() == 1
        def ldapLisi = accounts300A[0]
        assert ldapLisi != null

        def accounts300B = queryAccount {
            delegate.conditions = [
                "name=lisi", "source=" + AccountSource.Local.toString()
            ]
        } as List<AccountInventory>
        assert accounts300B.size() == 0

        logger.info("Test 301: set a local password on the WindowsAD account[lisi] while no same-name Local exists, expect success (not shadowed yet)")
        updateAccount {
            delegate.uuid = ldapLisi.uuid
            delegate.password = "lisipassword"
        }

        String lisiStored = dbFindByUuid(ldapLisi.uuid, AccountVO.class).password
        assert AccountPassword.isEncoded(lisiStored)
        assert AccountPassword.matches("lisipassword", lisiStored)
        def accounts301 = queryAccount {
            delegate.conditions = [
                "uuid=" + ldapLisi.uuid
            ]
        } as List<AccountInventory>
        assert accounts301.size() == 1

        logger.info("Test 302: local password login lisi without any same-name Local account, expect success (legacy behavior, hits the WindowsAD account)")
        SessionInventory legacySession = logInByAccount {
            delegate.accountName = "lisi"
            delegate.password = "lisipassword"
        } as SessionInventory
        assert legacySession.accountUuid == ldapLisi.uuid

        logger.info("Test 303: create a Local account[lisi], expect success and coexist with the WindowsAD one")
        def localLisi = createAccount {
            delegate.name = "lisi"
            delegate.password = "localpassword"
        } as AccountInventory
        assert localLisi.uuid != ldapLisi.uuid

        def accounts303 = queryAccount {
            delegate.conditions = [
                "name=lisi"
            ]
        } as List<AccountInventory>
        assert accounts303.size() == 2
        assert accounts303.count { it.uuid == localLisi.uuid && it.source == AccountSource.Local.toString() } == 1
        assert accounts303.count { it.source == AccountSource.WindowsAD.toString() } == 1

        logger.info("Test 304: local password login lisi with the WindowsAD password now that a same-name Local exists, expect login fail (shadowed by Local)")
        expectApiFailure({
            logInByAccount {
                delegate.accountName = "lisi"
                delegate.password = "lisipassword"
            }
        }) {
            assert code == "ID.1000"
        }

        logger.info("Test 305: local password login lisi with the Local password, expect success (hits the Local account)")
        SessionInventory localSession = logInByAccount {
            delegate.accountName = "lisi"
            delegate.password = "localpassword"
        } as SessionInventory
        assert localSession.accountUuid == localLisi.uuid

        logger.info("Test 306: update the password of the now-shadowed WindowsAD account[lisi], expect API failure")
        expectApiFailure({
            updateAccount {
                delegate.uuid = ldapLisi.uuid
                delegate.password = "lisipassword2"
            }
        }) {
            assert code == SysErrors.OPERATION_ERROR.toString()
        }

        assert dbFindByUuid(ldapLisi.uuid, AccountVO.class).password == lisiStored

        logger.info("Test 399: clean accounts named lisi")
        cleanupAccountsNamed("lisi")
        clearLdapUsers()

        assert !Q.New(AccountVO.class).eq(AccountVO_.@name, "lisi").isExists()
    }

    void testLoginEntryRejectsSourceAndUuid() {
        logger.info("Test 401: APILogInByAccountMsg(logInByAccount) must not expose source/accountUuid, expect no such API params")
        assert !hasField(LogInByAccountAction.class, "source")
        assert !hasField(LogInByAccountAction.class, "accountUuid")

        logger.info("Test 402: APILogInMsg(logIn) must not expose source/accountUuid, expect no such API params")
        assert !hasField(LogInAction.class, "source")
        assert !hasField(LogInAction.class, "accountUuid")
    }

    void testRegressZsv12290SameNameBinding() {
        logger.info("Test 501: create a local account[wangwu], expect success")
        def localWangwu = createAccount {
            delegate.name = "wangwu"
            delegate.password = "password"
        } as AccountInventory

        def accounts501A = queryAccount {
            delegate.conditions = [
                "uuid=" + localWangwu.uuid
            ]
        } as List<AccountInventory>
        assert accounts501A.size() == 1
        assert accounts501A[0].source == AccountSource.Local.toString()

        def accounts501B = queryAccount {
            delegate.conditions = [
                "name=wangwu"
            ]
        } as List<AccountInventory>
        assert accounts501B.size() == 1

        logger.info("Test 502: bind an LDAP user to the existing local account[wangwu] (ZSV-12290 reuse), expect success and NO new account created")
        installLdapUsers("wangwu")
        createLdapBinding {
            delegate.ldapUid = ldapDn("wangwu")
            delegate.accountUuid = localWangwu.uuid
        }

        def wangwus = queryAccount {
            delegate.conditions = ["name=wangwu"]
        } as List<AccountInventory>
        assert wangwus.size() == 1
        assert wangwus[0].uuid == localWangwu.uuid

        def bindings = (queryThirdPartyAccountSourceBinding {} as List<AccountThirdPartyAccountSourceRefInventory>)
                .findAll { it.accountUuid == localWangwu.uuid }
        assert bindings.size() == 1
        // verify at DB level: no new account row created, and the binding ref points to the local account
        assert Q.New(AccountVO.class).eq(AccountVO_.@name, "wangwu").count() == 1
        assert Q.New(AccountThirdPartyAccountSourceRefVO.class)
                .eq(AccountThirdPartyAccountSourceRefVO_.accountUuid, localWangwu.uuid)
                .count() == 1

        logger.info("Test 599: clean accounts named wangwu")
        cleanupAccountsNamed("wangwu")
        clearLdapUsers()
        // verify the account and its binding ref are both gone from DB after cleanup (FK cascade)
        assert !Q.New(AccountVO.class).eq(AccountVO_.@name, "wangwu").isExists()
        assert !Q.New(AccountThirdPartyAccountSourceRefVO.class)
                .eq(AccountThirdPartyAccountSourceRefVO_.accountUuid, localWangwu.uuid)
                .isExists()
    }

    @Override
    void clean() {
        env.delete()
    }

    // -------------------------------------  helpers

    private static String ldapDn(String cn) {
        return "CN=${cn},ou=test,dc=adtest,dc=zs".toString()
    }

    private void installLdapUsers(String... cns) {
        endpoint.installSearchHandler { spec ->
            cns.collect { cn ->
                endpoint.ldapEntry()
                        .enable()
                        .withDn(ldapDn(cn))
                        .withAttribute("name", cn)
                        .withAttribute("distinguishedName", ldapDn(cn))
                        .withAttribute("cn", cn)
                        .withAttributeSet("objectClass", "top", "person", "organizationalPerson", "user")
                        .build()
            }
        }
    }

    private void clearLdapUsers() {
        endpoint.clearSearchHandler()
        endpoint.clearAuthenticateHandler()
    }

    private void cleanupAccountsNamed(String... names) {
        names.each { name ->
            (queryAccount {
                delegate.conditions = ["name=" + name]
            } as List<AccountInventory>).each { acc ->
                deleteAccount {
                    delegate.uuid = acc.uuid
                }
            }
        }
    }

    private static boolean hasField(Class<?> clz, String fieldName) {
        for (Field f : clz.getFields()) {
            if (f.getName() == fieldName) {
                return true
            }
        }
        for (Field f : clz.getDeclaredFields()) {
            if (f.getName() == fieldName) {
                return true
            }
        }
        return false
    }
}
