package org.zstack.test.integration.storage.primary.smp.capacity

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.smp.SMPConstants
import org.zstack.test.integration.storage.SMPEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec

/**
 * Created by zouye on 2017/3/1.
 */
class SMPCapacityCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = SMPEnv.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testReconnectPrimaryStorageCapacityRecalculation()
            testReleaseSMPCapacityWithNoHostInCase()
        }
    }

    private void testReleaseSMPCapacityWithNoHostInCase() {
        HostSpec hostSpec =  env.specByName("kvm")
        ClusterSpec clusterSpec = env.specByName("cluster")
        PrimaryStorageSpec primaryStorageSpec = env.specByName("smp")
        VmSpec vmSpec = env.specByName("vm")
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        destroyVmInstance {
            uuid = vmSpec.inventory.uuid
        }
        deleteHost {
            uuid = hostSpec.inventory.uuid
        }

        List<String> psUuids = SQL.New("select pri.uuid" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid" +
                " and pri.type = :ptype")
                .param("cuuid", clusterSpec.inventory.uuid)
                .param("ptype", SMPConstants.SMP_TYPE)
                .list()

        assert !psUuids.isEmpty()

        List<String> hostUuids = Q.New(HostVO.class)
                                    .select(HostVO_.uuid).eq(HostVO_.clusterUuid, clusterSpec.inventory.uuid).listValues()
        assert hostUuids.isEmpty()

        PrimaryStorageCapacityVO vo
        retryInSecs {
            vo = dbFindByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageCapacityVO.class)
            assert vo.getAvailablePhysicalCapacity() == 0L
            assert vo.getAvailableCapacity() == 0L
            assert vo.getTotalPhysicalCapacity() == 0L
            assert vo.getTotalCapacity() == 0L
            assert vo.getSystemUsedCapacity() == 0L
        }

        PrimaryStorageVO primaryStorageVO
        retryInSecs {
            primaryStorageVO = dbFindByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class)
            assert primaryStorageVO.getCapacity().getTotalCapacity() == 0L
            assert primaryStorageVO.getCapacity().getSystemUsedCapacity() == 0L
            assert primaryStorageVO.getCapacity().getAvailableCapacity() == 0L
            assert primaryStorageVO.getCapacity().getTotalPhysicalCapacity() == 0L
            assert primaryStorageVO.getCapacity().getAvailablePhysicalCapacity() == 0L
        }
    }

    void testReconnectPrimaryStorageCapacityRecalculation() {
        PrimaryStorageInventory ps = env.inventoryByName("smp")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("iso")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")

        GetPrimaryStorageCapacityResult beforeCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        VmInstanceInventory vm = createVmInstance {
            name = "test"
            l3NetworkUuids = [l3.uuid]
            imageUuid = image.uuid
            rootDiskOfferingUuid = diskOffering.uuid
            instanceOfferingUuid = instanceOffering.uuid
        }

        GetPrimaryStorageCapacityResult afterCreateVmInstance = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacity.availableCapacity > afterCreateVmInstance.availableCapacity

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        GetPrimaryStorageCapacityResult afterReconnectPS = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert afterReconnectPS.availableCapacity == afterCreateVmInstance.availableCapacity
        assert afterReconnectPS.totalCapacity == afterCreateVmInstance.totalCapacity

        destroyVmInstance {
            uuid = vm.uuid
        }

        expungeVmInstance {
            uuid = vm.uuid
        }

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        GetPrimaryStorageCapacityResult afterReconnectPS2 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert afterReconnectPS2.availableCapacity == beforeCapacity.availableCapacity

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
