package org.zstack.test.integration.identity.imports

import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.identity.AccountType
import org.zstack.header.identity.CreateAccountMsg
import org.zstack.header.message.MessageReply
import org.zstack.header.message.NeedReplyMessage
import org.zstack.identity.imports.AccountImportsConstant
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy
import org.zstack.identity.imports.header.AbstractAccountSourceSpec
import org.zstack.identity.imports.header.ImportAccountItem
import org.zstack.identity.imports.header.ImportAccountSpec
import org.zstack.identity.imports.message.AccountSourceMessage
import org.zstack.identity.imports.message.CreateThirdPartyAccountSourceMsg
import org.zstack.identity.imports.message.ImportThirdPartyAccountMsg
import org.zstack.identity.imports.message.ImportThirdPartyAccountReply
import org.zstack.ldap.LdapConstant
import org.zstack.sdk.identity.imports.entity.AccountThirdPartyAccountSourceRefInventory
import org.zstack.sdk.identity.ldap.entity.LdapServerInventory
import org.zstack.test.integration.identity.IdentityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.identity.ldap.LdapVirtualEndpointSpec

import java.util.concurrent.atomic.AtomicReference

class AccountImportsCoverageCase extends SubCase {
    EnvSpec env
    LdapServerInventory ldap0, ldap1
    LdapVirtualEndpointSpec endpoint0, endpoint1

    CloudBus bus

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
                base = "dc=LdapMultiEndpointCase,dc=zsv"
                url = "ldap://test.zstack.io:10001"
                username = "AccountImportsCoverageCase"
                password = "none"
                encryption = "None"

                ldapServer {
                    name = "ldap0"
                    serverType = "WindowsAD"
                }
            }

            ldapEndpoint {
                name = "ldapEndpoint1"
                base = "dc=LdapMultiEndpointCase,dc=zsv"
                url = "ldap://test.zstack.io:10002"
                username = "AccountImportsCoverageCase"
                password = "none"
                encryption = "None"

                ldapServer {
                    name = "ldap1"
                    serverType = "WindowsAD"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testAccountImportsManagerCoverage()
            testAccountSourceBaseCoverage()
            testImportAccountFailed()
        }
    }

    void prepare() {
        bus = bean(CloudBus.class)
        ldap0 = env.inventoryByName("ldap0") as LdapServerInventory
        ldap1 = env.inventoryByName("ldap1") as LdapServerInventory
        endpoint0 = env.specByName("ldapEndpoint0") as LdapVirtualEndpointSpec
        endpoint1 = env.specByName("ldapEndpoint1") as LdapVirtualEndpointSpec
    }

    void testAccountImportsManagerCoverage() {
        logger.info("Test 101: testAccountImportsManagerCoverage")
        def done = new AtomicReference<MessageReply>()

        def message1 = new CreateAccountMsg()
        message1.uuid = Platform.getUuid()
        message1.name = "123"
        message1.password = Platform.getUuid()
        message1.type = "ThirdParty"
        bus.makeLocalServiceId(message1, AccountImportsConstant.SERVICE_ID)
        bus.send(message1, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done.set(reply)
            }
        })

        retryInSecs {
            assert done.get() != null
        }
        MessageReply messageReply = done.get()
        assert !messageReply.success
        assert messageReply.error.code == "SYS.1008"
        assert SysErrors.UNKNOWN_MESSAGE_ERROR.toString() == "SYS.1008"

        done.set(null)
        def message2 = new TestCreateThirdPartyAccountSourceMsg()
        message2.spec = new TestAbstractAccountSourceSpec()
        message2.spec.type = "wrongType"
        bus.makeLocalServiceId(message2, AccountImportsConstant.SERVICE_ID)
        bus.send(message2, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done.set(reply)
            }
        })

        retryInSecs {
            assert done.get() != null
        }
        messageReply = done.get()
        assert !messageReply.success
        assert messageReply.error.code == "SYS.1006"
        assert SysErrors.OPERATION_ERROR.toString() == "SYS.1006"
        assert messageReply.error.details.contains("[wrongType]")
    }

    void testAccountSourceBaseCoverage() {
        logger.info("Test 201: testAccountSourceBaseCoverage")
        def done = new AtomicReference<MessageReply>()

        def message1 = new TestAccountSourceMessage()
        message1.sourceUuid = ldap0.uuid
        bus.makeLocalServiceId(message1, AccountImportsConstant.SERVICE_ID)
        bus.send(message1, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done.set(reply)
            }
        })

        retryInSecs {
            assert done.get() != null
        }
        MessageReply messageReply = done.get()
        assert !messageReply.success
        assert messageReply.error.code == "SYS.1008"
        assert SysErrors.UNKNOWN_MESSAGE_ERROR.toString() == "SYS.1008"
    }

    void testImportAccountFailed() {
        def done = new AtomicReference<MessageReply>()

        endpoint0.installSearchHandler { spec -> [
            endpoint0.ldapEntry()
                    .withDn("CN=test-1,ou=测试,dc=adtest,dc=zs")
                    .withAttribute("name", "test-1")
                    .withAttribute("distinguishedName", "CN=test-1,ou=测试,dc=adtest,dc=zs")
                    .withAttribute("cn", "test-1")
                    .withAttributeSet("objectClass", "top", "person", "organizationalPerson", "user", "computer")
                    .build()
        ]}

        syncAccountsFromLdapServer {
            delegate.uuid = ldap0.uuid
        }

        def bindings = queryThirdPartyAccountSourceBinding {} as List<AccountThirdPartyAccountSourceRefInventory>
        assert bindings.size() == 1
        assert bindings[0].accountSourceUuid == ldap0.uuid

        logger.info("Test 301: Third party user has already binding to other account")
        def message1 = new ImportThirdPartyAccountMsg()
        message1.spec = new ImportAccountSpec()
        message1.spec.sourceUuid = ldap0.uuid
        message1.spec.sourceType = LdapConstant.LOGIN_TYPE
        message1.spec.syncCreateStrategy = SyncCreatedAccountStrategy.CreateAccount
        message1.spec.accountList = [new ImportAccountItem()]
        // account is already exists, so new accountUuid is not allowed
        message1.spec.accountList[0].accountUuid = Platform.getUuid()
        message1.spec.accountList[0].credentials = bindings[0].credentials
        message1.spec.accountList[0].accountType = AccountType.Normal
        bus.makeLocalServiceId(message1, AccountImportsConstant.SERVICE_ID)
        bus.send(message1, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done.set(reply)
            }
        })

        retryInSecs {
            assert done.get() != null
        }
        MessageReply messageReply = done.get()
        assert messageReply instanceof ImportThirdPartyAccountReply
        def reply1 = messageReply as ImportThirdPartyAccountReply
        assert reply1.success
        assert reply1.results.size() == 1
        assert reply1.results[0].ref == null
        assert reply1.results[0].error != null
        assert reply1.results[0].error.code == "SYS.1006"
        assert SysErrors.OPERATION_ERROR.toString() == "SYS.1006"
        assert reply1.results[0].error.details.contains(bindings[0].credentials)

        bindings = queryThirdPartyAccountSourceBinding {} as List<AccountThirdPartyAccountSourceRefInventory>
        assert bindings.size() == 1
        done.set(null)

        logger.info("Test 302: invalid account spec: accountUuid is null and createIfNotExist=false")
        def message2 = new ImportThirdPartyAccountMsg()
        message2.spec = new ImportAccountSpec()
        message2.spec.sourceUuid = ldap0.uuid
        message2.spec.sourceType = LdapConstant.LOGIN_TYPE
        message2.spec.accountList = [new ImportAccountItem()]
        message2.spec.accountList[0].accountUuid = null
        message2.spec.accountList[0].credentials = "CN=test-9999999,ou=测试,dc=adtest,dc=zs"
        message2.spec.accountList[0].accountType = AccountType.Normal
        bus.makeLocalServiceId(message2, AccountImportsConstant.SERVICE_ID)
        bus.send(message2, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done.set(reply)
            }
        })

        retryInSecs {
            assert done.get() != null
        }
        messageReply = done.get()
        assert messageReply instanceof ImportThirdPartyAccountReply
        def reply2 = messageReply as ImportThirdPartyAccountReply
        assert reply2.success
        assert reply2.results.size() == 1
        assert reply2.results[0].ref == null
        assert reply2.results[0].error != null
        assert reply2.results[0].error.code == "SYS.1006"
        assert SysErrors.OPERATION_ERROR.toString() == "SYS.1006"
        assert reply2.results[0].error.details == "invalid account spec: accountUuid is null"

        bindings = queryThirdPartyAccountSourceBinding {} as List<AccountThirdPartyAccountSourceRefInventory>
        assert bindings.size() == 1
        done.set(null)

        logger.info("Test 303: invalid account spec: failed to find account and createIfNotExist=false")
        def message3 = new ImportThirdPartyAccountMsg()
        message3.spec = new ImportAccountSpec()
        message3.spec.sourceUuid = ldap0.uuid
        message3.spec.sourceType = LdapConstant.LOGIN_TYPE
        message3.spec.accountList = [new ImportAccountItem()]
        message3.spec.accountList[0].accountUuid = Platform.getUuid()
        message3.spec.accountList[0].credentials = "CN=test-9999999,ou=测试,dc=adtest,dc=zs"
        message3.spec.accountList[0].accountType = AccountType.Normal
        bus.makeLocalServiceId(message3, AccountImportsConstant.SERVICE_ID)
        bus.send(message3, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done.set(reply)
            }
        })

        retryInSecs {
            assert done.get() != null
        }
        messageReply = done.get()
        assert messageReply instanceof ImportThirdPartyAccountReply
        def reply3 = messageReply as ImportThirdPartyAccountReply
        assert reply3.success
        assert reply3.results.size() == 1
        assert reply3.results[0].ref == null
        assert reply3.results[0].error != null
        assert reply3.results[0].error.code == "SYS.1006"
        assert SysErrors.OPERATION_ERROR.toString() == "SYS.1006"
        assert reply3.results[0].error.details ==
                "invalid account spec: failed to find account[uuid=${message3.spec.accountList[0].accountUuid}]".toString()

        bindings = queryThirdPartyAccountSourceBinding {} as List<AccountThirdPartyAccountSourceRefInventory>
        assert bindings.size() == 1
        done.set(null)

        logger.info("Test 304: account has already binding other third party source")
        def message4 = new ImportThirdPartyAccountMsg()
        message4.spec = new ImportAccountSpec()
        message4.spec.sourceUuid = ldap1.uuid // other ldap source
        message4.spec.sourceType = LdapConstant.LOGIN_TYPE
        message4.spec.accountList = [new ImportAccountItem()]
        message4.spec.accountList[0].accountUuid = bindings[0].accountUuid // account has already binding other source
        message4.spec.accountList[0].credentials = "CN=test-1,ou=测试,dc=adtest,dc=zs"
        message4.spec.accountList[0].accountType = AccountType.Normal
        bus.makeLocalServiceId(message4, AccountImportsConstant.SERVICE_ID)
        bus.send(message4, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done.set(reply)
            }
        })

        retryInSecs {
            assert done.get() != null
        }
        messageReply = done.get()
        assert messageReply instanceof ImportThirdPartyAccountReply
        def reply4 = messageReply as ImportThirdPartyAccountReply
        assert reply4.success
        assert reply4.results.size() == 1
        assert reply4.results[0].ref == null
        assert reply4.results[0].error != null
        assert reply4.results[0].error.code == "SYS.1006"
        assert SysErrors.OPERATION_ERROR.toString() == "SYS.1006"
        assert reply4.results[0].error.details ==
                "account[uuid=${message4.spec.accountList[0].accountUuid}] has already binding other third party source".toString()

        bindings = queryThirdPartyAccountSourceBinding {} as List<AccountThirdPartyAccountSourceRefInventory>
        assert bindings.size() == 1
        done.set(null)

        logger.info("Test 305: create account fail")
        env.message(CreateAccountMsg) { msg ->
            throw new CloudRuntimeException("on purpose")
        }

        def message5 = new ImportThirdPartyAccountMsg()
        message5.spec = new ImportAccountSpec()
        message5.spec.sourceUuid = ldap0.uuid
        message5.spec.sourceType = LdapConstant.LOGIN_TYPE
        message5.spec.syncCreateStrategy = SyncCreatedAccountStrategy.CreateAccount
        message5.spec.accountList = [new ImportAccountItem()]
        message5.spec.accountList[0].accountUuid = Platform.getUuid()
        message5.spec.accountList[0].credentials = "CN=test-2,ou=测试,dc=adtest,dc=zs" // new credentials
        message5.spec.accountList[0].accountType = AccountType.Normal
        bus.makeLocalServiceId(message5, AccountImportsConstant.SERVICE_ID)
        bus.send(message5, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done.set(reply)
            }
        })

        retryInSecs {
            assert done.get() != null
        }
        messageReply = done.get()
        assert messageReply instanceof ImportThirdPartyAccountReply
        def reply5 = messageReply as ImportThirdPartyAccountReply
        assert reply5.success
        assert reply5.results.size() == 1
        assert reply5.results[0].ref == null
        assert reply5.results[0].error != null
        assert reply5.results[0].error.details == "on purpose"

        bindings = queryThirdPartyAccountSourceBinding {} as List<AccountThirdPartyAccountSourceRefInventory>
        assert bindings.size() == 1
        done.set(null)

        for (def binding in bindings) {
            deleteAccount {
                delegate.uuid = binding.accountUuid
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    class TestCreateThirdPartyAccountSourceMsg extends CreateThirdPartyAccountSourceMsg {
        TestAbstractAccountSourceSpec spec
    }

    class TestAbstractAccountSourceSpec extends AbstractAccountSourceSpec {}

    class TestAccountSourceMessage extends NeedReplyMessage implements AccountSourceMessage {
        String sourceUuid
    }
}
