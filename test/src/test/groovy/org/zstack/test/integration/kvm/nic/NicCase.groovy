package org.zstack.test.integration.kvm.nic

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.thread.AsyncThread
import org.zstack.header.image.ImagePlatform
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vm.VmNicVO
import org.zstack.sdk.AttachL3NetworkToVmAction
import org.zstack.sdk.DetachL3NetworkFromVmAction
import org.zstack.sdk.GetIpAddressCapacityResult
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.InstanceOfferingSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by AlanJager on 2017/3/23.
 */
class NicCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
          env = Env.oneVmTwoL3NetworkLocalEnv()
    }

    @Override
    void test() {
        env.create {
            testDetachNicConcurrently()
            testDetachNicFromVm()
        }
    }

    void testDetachNicConcurrently() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        GetIpAddressCapacityResult result = getIpAddressCapacity {
            l3NetworkUuids = [pubL3.uuid]
        }

        int num = 0
        int count = 50

        final CountDownLatch latch = new CountDownLatch(count)
        for (int i = 0; i < count; i++) {
            new Runnable() {
                @Override
                @AsyncThread
                void run() {
                    try {
                        AttachL3NetworkToVmAction action = new AttachL3NetworkToVmAction()
                        action.vmInstanceUuid = vm.uuid
                        action.l3NetworkUuid = pubL3.uuid
                        action.sessionId = adminSession()

                        AttachL3NetworkToVmAction.Result ret = action.call()
                        if (ret.error != null) {
                            throw new Exception()
                        }
                        num++
                    } catch (Exception e) {
                        logger.warn(e.getMessage(), e);
                    } finally {
                        latch.countDown()
                    }
                }
            }.run()
        }
        latch.await(120, TimeUnit.SECONDS)

        VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        final CountDownLatch latch1 = new CountDownLatch(vmvo.getVmNics().size())
        for (final VmNicVO nic : vmvo.getVmNics()) {
            new Runnable() {
                @Override
                @AsyncThread
                void run() {
                    try {
                        DetachL3NetworkFromVmAction action = new DetachL3NetworkFromVmAction()
                        action.vmNicUuid = nic.uuid
                        action.sessionId = adminSession()

                        DetachL3NetworkFromVmAction.Result ret = action.call()
                        if (ret.error != null) {
                            throw new Exception()
                        }
                        num++
                    } catch (Exception e) {
                        logger.warn(e.getMessage(), e)
                    } finally {
                        latch1.countDown()
                    }
                }
            }.run()
        }
        latch1.await(120, TimeUnit.SECONDS)

        GetIpAddressCapacityResult result2 = getIpAddressCapacity {
            l3NetworkUuids = [pubL3.uuid]
        }

        assert result.availableCapacity == result2.availableCapacity
    }

    void testDetachNicFromVm() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3")
        ImageSpec image = env.specByName("image")
        InstanceOfferingSpec instanceOffering = env.specByName("instanceOffering")

        for (imagePlatform in ImagePlatform.values()) {
            VmInstanceInventory vm = createVmInstance {
                name = "vm-${imagePlatform.toString()}"
                imageUuid = image.inventory.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = instanceOffering.inventory.uuid
            }

            SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).set(VmInstanceVO_.platform, imagePlatform.toString()).update()

            assert vm.getVmNics().size() == 1
            
            detachL3NetworkFromVm {
                vmNicUuid = vm.getVmNics().get(0).uuid
            }

            vm = queryVmInstance {
                conditions = ["uuid=${vm.uuid}"]
            }[0]
            assert vm.getVmNics().size() == 0

            attachL3NetworkToVm {
                l3NetworkUuid = l3.uuid
                vmInstanceUuid = vm.uuid
            }

            vm = queryVmInstance {
                conditions = ["uuid=${vm.uuid}"]
            }[0]
            assert vm.getVmNics().size() == 1

            stopVmInstance {
                uuid = vm.uuid
            }

            vm = queryVmInstance {
                conditions = ["uuid=${vm.uuid}"]
            }[0]
            assert VmInstanceState.Stopped.toString() == vm.getState()
            assert vm.getVmNics().size() == 1

            detachL3NetworkFromVm {
                vmNicUuid = vm.getVmNics().get(0).uuid
            }

            vm = queryVmInstance {
                conditions = ["uuid=${vm.uuid}"]
            }[0]
            assert vm.getVmNics().size() == 0

            startVmInstance {
                uuid = vm.uuid
            }

            stopVmInstance {
                uuid = vm.uuid
            }

            destroyVmInstance {
                uuid = vm.uuid
            }

            vm = queryVmInstance {
                conditions = ["uuid=${vm.uuid}"]
            }[0]
            assert VmInstanceState.Destroyed.toString() == vm.getState()
            assert vm.getVmNics().size() == 0

            expungeVmInstance{
                uuid = vm.uuid
            }
        }
    }
}
