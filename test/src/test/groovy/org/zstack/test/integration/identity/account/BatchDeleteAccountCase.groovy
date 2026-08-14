package org.zstack.test.integration.identity.account

import org.zstack.core.db.Q
import org.zstack.sdk.*
import org.zstack.header.identity.*
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.identity.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by lining on 2020/4/24.
 */
class BatchDeleteAccountCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testBatchDeleteAccount()
        }
    }

    void testBatchDeleteAccount() {
        List<String> accountUuids = []
        String accountNamePre = "newAccount"
        for (int i = 2; i < 52; i++) {
            def accountName = String.format("%s.%d", accountNamePre,i)

            AccountInventory accountInventory = createAccount {
                name = accountName
                password = "password"
            } as AccountInventory

            accountUuids.add(accountInventory.uuid)
        }

        AtomicInteger count = new AtomicInteger(0)
        def threads = []

        for (String hostUuid : accountUuids) {
            String uuid = hostUuid
            def thread = Thread.start {
                DeleteAccountAction action = new DeleteAccountAction(
                        uuid: uuid,
                        sessionId: Test.currentEnvSpec.session.uuid,
                )
                action.call()
                count.incrementAndGet()
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        retryInSecs(15, 3) {
            assert count.get() == accountUuids.size()
        }

        assert Q.New(AccountVO.class)
                .like(AccountVO_.name, "${accountNamePre}%".toString())
                .count() == 0L
    }
}
