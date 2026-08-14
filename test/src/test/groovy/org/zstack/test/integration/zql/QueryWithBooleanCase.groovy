package org.zstack.test.integration.zql

import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class QueryWithBooleanCase extends SubCase {
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
        env = env {
        }
    }

    @Override
    void test() {
        env.create {
            testQueryWithBoolean()
        }
    }

    void testQueryWithBoolean(){
        def account = createAccount {
            name = "account"
            password = "password"
        } as AccountInventory

        def session = logInByAccount {
            accountName = account.name
            password = "password"
        } as SessionInventory

        queryVolume {
            conditions = ['isShareable=true']
            sessionId = session.uuid
        }
    }
}
