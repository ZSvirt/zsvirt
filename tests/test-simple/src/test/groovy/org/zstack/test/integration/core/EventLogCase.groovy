package org.zstack.test.integration.core

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.eventlog.EventLogType
import org.zstack.core.eventlog.EventLogVO
import org.zstack.core.eventlog.EventLogVO_
import org.zstack.core.eventlog.L
import org.zstack.testlib.SubCase

class EventLogCase extends SubCase {
    @Override
    void clean() {
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        testEventLog()
    }

    void testEventLog() {
        def uuid = "9def7935f2d04d078c81e69d3c79c0bf"
        def trackingId = "8408c5b115ad47118b65276a09f7bc68"
        def now = System.currentTimeMillis()

        L.New("test-core", EventLogCase.class, uuid)
                .trackingId(trackingId)
                .info_("this is %s", "fun")

        DatabaseFacade dbf = bean(DatabaseFacade.class)

        retryInSecs(2, 1) {
            def logs = Q.New(EventLogVO.class).eq(EventLogVO_.category, "test-core").list()
            assert logs.size() == 1

            EventLogVO vo = logs[0] as EventLogVO
            assert vo.getCategory() == "test-core"
            assert vo.getContent() == "this is fun"
            assert vo.getTime() >= now
            assert vo.getCreateDate() != null
            assert vo.getId() != null
            assert vo.getResourceType() == EventLogCase.class.getSimpleName()
            assert vo.getResourceUuid() == uuid
            assert vo.getTrackingId() == trackingId
            assert vo.getType() == EventLogType.Info
            dbf.remove(vo)
        }
    }

}

