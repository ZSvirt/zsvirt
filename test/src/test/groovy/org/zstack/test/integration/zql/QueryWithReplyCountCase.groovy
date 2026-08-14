package org.zstack.test.integration.zql

import org.zstack.sdk.QueryZoneAction
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class QueryWithReplyCountCase extends SubCase {
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
            testQueryWithReplyCount()
        }
    }

    void testQueryWithReplyCount() {
        for (int i = 0; i < 100; i++) {
            createZone {
                name = "demo"
            }
        }

        def action = new QueryZoneAction()
        action.replyWithCount = true
        action.sessionId = adminSession()
        action.start = 0
        action.limit = 50
        def r = action.call()
        assert r.value.total == 100
        assert r.value.inventories.size() == 50
    }
}