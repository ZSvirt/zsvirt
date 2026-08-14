package org.zstack.test.integration.core

import org.zstack.header.message.APIMessage
import org.zstack.sdk.DeleteZoneAction
import org.zstack.sdk.ZoneInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by kayo on 2017/9/21.
 */
class APIResourceRelatedFieldCheckCase extends SubCase{
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
                name = "zone"
            } as ZoneInventory

            DeleteZoneAction action = new DeleteZoneAction()
            action.uuid = zone.uuid + "test2321"
            action.sessionId = adminSession()
            DeleteZoneAction.Result ret = action.call()

            assert ret.error.details.contains("invalid value")
            assert ret.error.details.contains("of field")
        }
    }
}
