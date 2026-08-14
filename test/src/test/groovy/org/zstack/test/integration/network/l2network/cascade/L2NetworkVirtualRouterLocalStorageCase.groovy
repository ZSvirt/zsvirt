package org.zstack.test.integration.network.l2network.cascade

import junit.framework.Assert
import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by heathhose on 17-3-24.
 */
class L2NetworkVirtualRouterLocalStorageCase extends SubCase{
    def DOC = """
use:
1. have two clusters
2. create a vm with virtual router
3. make vr nowhere to migrate(in deploy configuration)
4. detach l2 which vr has l3 on
5. confirm vr is kill
"""
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        spring {
            include("eip.xml")
        }
    }

    @Override
    void environment() {
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnEipEnv()
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            detachL2AndCheckVRState()
        }
    }

    void detachL2AndCheckVRState(){
        L2NetworkInventory l2i = env.inventoryByName("l2")
        ClusterInventory cluster = env.inventoryByName("cluster")

        ApplianceVmVO vr = dbf.listAll(ApplianceVmVO.class).get(0)
        Assert.assertEquals(VmInstanceState.Running, vr.getState()) 
        long count = dbf.count(VmInstanceVO.class) 
        assert count == 2

        detachL2NetworkFromCluster {
            l2NetworkUuid = l2i.uuid
            clusterUuid = cluster.uuid
        }

        count = dbf.count(VmInstanceVO.class) 
        assert count == 1
    }

    @Override
    void clean() {
        env.delete()
    }
}