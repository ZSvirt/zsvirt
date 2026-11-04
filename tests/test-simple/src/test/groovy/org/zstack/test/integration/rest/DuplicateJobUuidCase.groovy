package org.zstack.test.integration.rest


import org.zstack.header.network.l2.L2NetworkConstant
import org.zstack.sdk.CreateL2NoVlanNetworkAction
import org.zstack.sdk.QueryL2NetworkAction
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.SimpleTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by kayo on 2017/9/7.
 */
class DuplicateJobUuidCase extends SubCase {
    EnvSpec env
    ZoneInventory zoneInventory

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(SimpleTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
                description = "test"
            }
        }
    }

    @Override
    void test() {
        env.create {
            zoneInventory = env.inventoryByName("zone") as ZoneInventory
            testDuplicateApiId()
        }
    }

    void testDuplicateApiId() {
        CreateL2NoVlanNetworkAction action = new CreateL2NoVlanNetworkAction()
        action.sessionId = adminSession()
        action.name = "test l2"
        action.description = "test"
        action.zoneUuid = zoneInventory.uuid
        action.physicalInterface = "eth0"
        action.apiId = UUID.randomUUID().toString().replace("-", "")
        action.type = L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE;
        CreateL2NoVlanNetworkAction.Result res = action.call();

        res = action.call();
        assert res.error.details.contains("Duplicate job uuid")

        QueryL2NetworkAction qa = new QueryL2NetworkAction()
        qa.sessionId = adminSession()
        qa.apiId = UUID.randomUUID().toString().replace("-", "")
        QueryL2NetworkAction.Result qr = qa.call()

        qr = qa.call()
        assert qr.error == null
    }
}
