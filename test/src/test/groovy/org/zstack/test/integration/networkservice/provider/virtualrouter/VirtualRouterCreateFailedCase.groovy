package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.appliancevm.StartNewCreatedApplianceVmMsg
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.vm.InstantiateNewCreatedVmInstanceReply
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.core.Platform.operr

/**
 * Created by shixin on 01/12/19.
 */
class VirtualRouterCreateFailedCase extends SubCase {
    EnvSpec env
    CloudBus bus

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = VirtualRouterNetworkServiceEnv.ForHostsVyosOnEipEnv()
    }

    @Override
    void test() {
        env.create {
            bus = bean(CloudBus.class)
            testCreateVrouterRollback()
        }
    }

    void testCreateVrouterRollback() {
        def l3nw = env.inventoryByName("l3") as L3NetworkInventory
        def image = env.inventoryByName("image") as ImageInventory
        def offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory

        env.message(StartNewCreatedApplianceVmMsg.class) { StartNewCreatedApplianceVmMsg msg, CloudBus bus ->
            InstantiateNewCreatedVmInstanceReply reply = new InstantiateNewCreatedVmInstanceReply()
            reply.setError(operr("on purpose"))
            bus.reply(msg, reply)
        }

        expect (AssertionError.class) {
            createVmInstance {
                name = "test-vm"
                instanceOfferingUuid = offer.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3nw.uuid]
            }
        }

        assert Q.New(ApplianceVmVO.class).count() == 0

    }
}
