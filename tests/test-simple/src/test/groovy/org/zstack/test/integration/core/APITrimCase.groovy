package org.zstack.test.integration.core

import org.zstack.sdk.ZoneInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by MaJin on 2017-08-09.
 */
class APITrimCase extends SubCase{
    EnvSpec env
    ZoneInventory zone

    @Override
    void clean() {
        deleteZone {
            uuid = zone.uuid
        }

        env.delete()
    }

    @Override
    void setup() {
        spring{
            include("ZoneManager.xml")
        }
    }

    @Override
    void environment() {
        env = makeEnv{}
    }

    @Override
    void test() {
        env.create {
            zone = createZone {
                name = " test"
            } as ZoneInventory
            assert zone.name == "test"
        }
    }
}
