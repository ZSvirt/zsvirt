package org.zstack.test.integration.identity.account

import org.zstack.header.identity.IdentityErrors
import org.zstack.sdk.AccountInventory
import org.zstack.test.integration.identity.IdentityTest
import org.zstack.testlib.FSMCase

class AccountFSMCase extends FSMCase {
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
        }

        state("init") {
            markAsInitialState()
        }

        state("state=enabled") {
            checker {
                def accountUuid = getNotNull("account") as String
                def accounts = queryAccount {
                    delegate.conditions = ["uuid=" + accountUuid]
                } as List<AccountInventory>
                assert accounts.size() == 1
                assert accounts[0].state == "Enabled"
            }
            checker {
                logInByAccount {
                    delegate.accountName = "xxx"
                    delegate.password = "yyy"
                }
            }
        }

        state("state=disabled") {
            checker {
                def accountUuid = getNotNull("account") as String
                def accounts = queryAccount {
                    delegate.conditions = ["uuid=" + accountUuid]
                } as List<AccountInventory>
                assert accounts.size() == 1
                assert accounts[0].state == "Disabled"
            }
            checker {
                expectApiFailure({
                    logInByAccount {
                        delegate.accountName = "xxx"
                        delegate.password = "yyy"
                    }
                }) {
                    assert delegate.code == "ID.1007"
                    assert IdentityErrors.ACCOUNT_DISABLED.toString() == "ID.1007"
                }
            }
        }

        action("create account") {
            run = { createAccountAction() }
        }

        action("enable account") {
            run = { enableAccountAction() }
        }

        action("disable account") {
            run = { disableAccountAction() }
        }

        event {
            action = "create account"
            fromState = "init"
            toState = "state=enabled"
        }

        event {
            action = "disable account"
            fromState = "state=enabled"
            toState = "state=disabled"
        }

        event {
            action = "enable account"
            fromState = "state=disabled"
            toState = "state=enabled"
        }
    }

    void createAccountAction() {
        def account = createAccount {
            delegate.name = "xxx"
            delegate.password = "yyy"
        } as AccountInventory
        save("account", account.uuid)
    }

    void enableAccountAction() {
        def accountUuid = getNotNull("account") as String
        updateAccount {
            delegate.uuid = accountUuid
            delegate.state = "Enabled"
        }
    }

    void disableAccountAction() {
        def accountUuid = getNotNull("account") as String
        updateAccount {
            delegate.uuid = accountUuid
            delegate.state = "Disabled"
        }
    }
}
