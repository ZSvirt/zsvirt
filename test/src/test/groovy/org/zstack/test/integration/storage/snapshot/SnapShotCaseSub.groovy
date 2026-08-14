package org.zstack.test.integration.storage.snapshot

import org.zstack.sdk.SessionInventory
import org.zstack.testlib.SubCase

abstract class SnapShotCaseSub extends SubCase{

    void withAccountSession(String accountName, String password, Closure c) {
        SessionInventory session = logInByAccount {
            delegate.accountName = accountName
            delegate.password = password
        }

        withSession(session, c)
    }
}
