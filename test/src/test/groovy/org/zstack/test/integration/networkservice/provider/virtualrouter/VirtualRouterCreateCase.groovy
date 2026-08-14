package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by david on 3/21/17.
 */
class VirtualRouterCreateCase extends SubCase {
    EnvSpec env

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
        // This environment contains vr-offering but no VM.
        env = VirtualRouterNetworkServiceEnv.ForHostsVyosOnEipEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateMultipleVmWithVrouterNetwork()
        }
    }

    void testCreateMultipleVmWithVrouterNetwork() {
        def l3nw = env.inventoryByName("l3") as L3NetworkInventory
        def image = env.inventoryByName("image") as ImageInventory
        def offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def threads = []

        // We have 4 hosts, with each has 2 CPU, 4 GiB memory.
        // The offering is 5 CPU x 512 MiB, where VR consumes 2 CPU x 512MiB
        // We shall be able to create another (4 x 2 x 10 - 2) / 5 = 15 hosts.
        long numberOfVmToCreate = 15
        for (idx in 1..numberOfVmToCreate) {
            def thread = Thread.start {
                def vmName = "VM-${idx}".toString()
                try {
                    createVmInstance {
                        name = vmName
                        instanceOfferingUuid = offer.uuid
                        imageUuid = image.uuid
                        l3NetworkUuids = [l3nw.uuid]
                    }
                } catch (AssertionError ignored) {
                }
            }
            threads.add(thread)
        }

        threads.each {it.join()}

        DatabaseFacade dbf = bean(DatabaseFacade.class)
        def numberOfVm = dbf.count(VmInstanceVO.class)
        assert numberOfVm == numberOfVmToCreate + 1
    }
}
