package org.zstack.test.integration.core.database

import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.zone.ZoneState
import org.zstack.header.zone.ZoneVO
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

class EoCleanupCase extends SubCase {
    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testEoCleanup()
    }

    void testEoCleanup() {
        DatabaseFacade dbf =  bean(DatabaseFacade.class)
        String num = System.getProperty("num")
        int numberOfZones = num == null ? 2000 : Integer.parseInt(num)

        List<ZoneVO> zones = new ArrayList<>()
        for (int i = 0; i < numberOfZones; i++) {
            ZoneVO z = new ZoneVO()
            z.uuid = Platform.uuid
            z.name = "zone" + i
            z.type = "zstack"
            z.state = ZoneState.Enabled
            zones.add(z)
        }

        logger.info("XXX: persisting $numberOfZones zones")
        dbf.persistCollection(zones)
        logger.info("XXX: persisted $numberOfZones zones")

        def t = Thread.start {
            logger.info("XXX: removing $numberOfZones zones")
            dbf.removeCollection(zones, ZoneVO.class)
            logger.info("XXX: removed $numberOfZones zones")
        }

        for (int i = 0; i < 3; i++) {
            TimeUnit.MICROSECONDS.sleep(50)
            logger.info("XXX: cleaning up $numberOfZones zones")
            dbf.eoCleanup(ZoneVO.class)
            logger.info("XXX: cleaned up $numberOfZones zones")
        }

        t.join()

        dbf.eoCleanup(ZoneVO.class)
        assert dbf.count(ZoneVO.class) == 0
    }

    @Override
    void clean() {
    }
}
