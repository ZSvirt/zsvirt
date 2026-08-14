package org.zstack.test.integration.networkservice.provider.flat.eip

import org.zstack.core.gc.GCStatus
import org.zstack.network.service.flat.BridgeNameFinder
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.sdk.EipInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EipSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/3/6.
 */
class FlatNetworkGCCase extends SubCase {
    EnvSpec env

    EipInventory eip
    HostInventory host
    L3NetworkInventory l3

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
        env = FlatNetworkServiceEnv.oneFlatEipEnv()
    }


    void testEipGCSuccess() {
        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteEip {
            uuid = eip.uuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
             inv = queryGCJob {
                conditions=["context~=%${eip.guestIp}%".toString(), "runnerClass=org.zstack.network.service.flat.FlatEipGC"]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }


        boolean called = false
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            called = true
            return rsp
        }

        // trigger the GC
        reconnectHost {
            uuid = host.uuid
        }

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${eip.guestIp}%".toString(), "runnerClass=org.zstack.network.service.flat.FlatEipGC"]
            }[0]

            assert called
            assert inv.status == GCStatus.Done.toString()
        }

        // clean the GC job so it won't effect following cases
        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testEipGCCancelledAfterHostDeleted() {
        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteEip {
            uuid = eip.uuid
        }

        GarbageCollectorInventory inv

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${eip.guestIp}%".toString(),"runnerClass=org.zstack.network.service.flat.FlatEipGC"]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        boolean called = false
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            called = true
            return rsp
        }

        deleteHost {
            uuid = host.uuid
        }

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${eip.guestIp}%".toString(),"runnerClass=org.zstack.network.service.flat.FlatEipGC"]
            }[0]

            assert !called
            assert inv.status == GCStatus.Done.toString()
        }

        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testNamespaceGCSuccess() {
        env.afterSimulator(FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        String bridgeName = new BridgeNameFinder().findByL3Uuid(l3.uuid)

        deleteL3Network {
            uuid = l3.uuid
        }

        GarbageCollectorInventory inv
        retryInSecs {
             inv = queryGCJob {
                conditions=["context~=%${host.uuid}%$bridgeName%".toString(), "runnerClass=org.zstack.network.service.flat.FlatDHCPDeleteNamespaceGC"]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        boolean called = false
        env.afterSimulator(FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH) { rsp ->
            called = true
            return rsp
        }

        reconnectHost {
            uuid = host.uuid
        }

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%$bridgeName%".toString(), "runnerClass=org.zstack.network.service.flat.FlatDHCPDeleteNamespaceGC"]
            }[0]

            assert called
            assert inv.status == GCStatus.Done.toString()
        }

        // cleanup the job for following cases
        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testNamespaceCancelledAfterHostDeleted() {
        env.afterSimulator(FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        String bridgeName = new BridgeNameFinder().findByL3Uuid(l3.uuid)

        deleteL3Network {
            uuid = l3.uuid
        }

        GarbageCollectorInventory inv

        retryInSecs {
             inv = queryGCJob {
                conditions=["context~=%${host.uuid}%$bridgeName%${l3.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        boolean called = false
        env.afterSimulator(FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH) { rsp ->
            called = true
            return rsp
        }

        deleteHost {
            uuid = host.uuid
        }

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%$bridgeName%".toString()]
            }[0]

            assert !called
            assert inv.status == GCStatus.Done.toString()
        }
    }

    @Override
    void test() {
        env.create {
            eip = (env.specByName("eip") as EipSpec).inventory
            host = (env.specByName("kvm") as HostSpec).inventory
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory

            testEipGCSuccess()

            eip = (env.recreate("eip") as EipSpec).inventory

            testEipGCCancelledAfterHostDeleted()

            host = (env.recreate("kvm") as HostSpec).inventory

            testNamespaceGCSuccess()

            l3 = (env.recreate("l3") as L3NetworkSpec).inventory

            testNamespaceCancelledAfterHostDeleted()
        }
    }
}
