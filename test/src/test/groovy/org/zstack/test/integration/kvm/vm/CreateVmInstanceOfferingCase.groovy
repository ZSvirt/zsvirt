package org.zstack.test.integration.kvm.vm

import org.zstack.header.configuration.InstanceOfferingStateEvent
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by MaJin on 2017-07-07.
 */
class CreateVmInstanceOfferingCase extends SubCase{
    EnvSpec env
    InstanceOfferingInventory userIns
    ImageInventory img
    L3NetworkInventory l3

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmEnv()
    }

    @Override
    void test() {
        env.create {
            userIns = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            img = env.inventoryByName("image1") as ImageInventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            testCreateVmUseDisabledInstanceOffering()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testCreateVmUseDisabledInstanceOffering(){
        changeInstanceOfferingState {
            uuid = userIns.uuid
            stateEvent = InstanceOfferingStateEvent.disable
        }

        expectApiFailure({
            createVmInstance {
                delegate.name = "test"
                delegate.instanceOfferingUuid = userIns.uuid
                delegate.imageUuid = img.uuid
                delegate.l3NetworkUuids = [l3.uuid]
            }
        }) {
            assert delegate.code == "SYS.1006"
        }
    }
}
