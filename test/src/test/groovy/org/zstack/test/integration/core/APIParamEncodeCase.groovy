package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.rest.RESTApiDecoder
import org.zstack.header.zone.APICreateZoneEvent
import org.zstack.header.zone.APICreateZoneMsg
import org.zstack.header.zone.ZoneInventory
import org.zstack.header.zone.ZoneState
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.GsonUtil
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by kayo on 2018/3/22.
 */
class APIParamEncodeCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        spring{
            include("ZoneManager.xml")
            include("CloudBusAopProxy.xml")
        }
    }

    @Override
    void environment() {
        env = makeEnv{}
    }

    @Override
    void test() {
        env.create {
            def testZoneName = "="

            env.message(APICreateZoneMsg.class) { APICreateZoneMsg msg, CloudBus bus ->
                assert msg.getName() == testZoneName

                def inv = new ZoneInventory()
                inv.setUuid(Platform.getUuid())
                inv.setName(testZoneName)
                inv.setState(ZoneState.Enabled.toString())
                def evt = new APICreateZoneEvent(msg.getId())
                evt.setInventory(inv)

                bus.publish(evt)
            }

            createZone {
                name = testZoneName
            }

            def encodedName = JSONObjectUtil.toJsonString(testZoneName)

            testZoneName = "\"=\""

            createZone {
                name = encodedName
            }
        }
    }
}
