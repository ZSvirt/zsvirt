package org.zstack.test.integration.network.vxlanNetwork

import org.apache.commons.collections.list.SynchronizedList
import org.springframework.http.HttpEntity
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.network.l2.vxlan.vtep.RemoteVtepVO;
import org.zstack.network.l2.vxlan.vtep.RemoteVtepVO_;
import org.zstack.sdk.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.core.db.Q
import java.util.stream.Collectors
import org.zstack.sdk.ApiException


class AddRemoteVxlanVtepIpCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    attachPrimaryStorage("local")

                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    kvm {
                        name = "kvm4"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    attachPrimaryStorage("nfs-ps")

                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                nfsPrimaryStorage {
                    name = "nfs-ps"
                    url = "localhost:/nfs"
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testRemoteVxlanVtepIp()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testRemoteVxlanVtepIp() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster1") as ClusterInventory
        def cluster2 = env.inventoryByName("cluster2") as ClusterInventory
        def host1 = env.inventoryByName("kvm1") as KVMHostInventory
        def host2 = env.inventoryByName("kvm2") as KVMHostInventory
        def host3 = env.inventoryByName("kvm3") as KVMHostInventory
        def host4 = env.inventoryByName("kvm4") as KVMHostInventory

        def pool = createL2VxlanNetworkPool {
            name = "TestVxlanPool1"
            type = "VxlanNetworkPool"
            zoneUuid = zone.uuid
        } as L2VxlanNetworkPoolInventory

        createVniRange {
            startVni = 100
            endVni = 10000
            l2NetworkUuid = pool.uuid
            name = "TestVxlanPool1"
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse() as VxlanKvmAgentCommands.CheckVxlanCidrResponse
            resp.setSuccess(true)
            return resp
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster.uuid
            systemTags = ["l2NetworkUuid::${pool.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{192.168.0.0/16}".toString()]
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster2.uuid
            systemTags = ["l2NetworkUuid::${pool.getUuid()}::clusterUuid::${cluster2.uuid}::cidr::{192.127.0.0/16}".toString()]
        }

        createVxlanPoolRemoteVtep {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster.uuid
            remoteVtepIp = "1.1.1.1"
        }
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).isExists()
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).eq(RemoteVtepVO_.clusterUuid,cluster.uuid).isExists()
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).count() == 1
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).eq(RemoteVtepVO_.clusterUuid,cluster.uuid).count() == 1 

        createVxlanPoolRemoteVtep {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster2.uuid
            remoteVtepIp = "1.1.1.1"
        }
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).isExists()
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).eq(RemoteVtepVO_.clusterUuid,cluster2.uuid).isExists()
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).count() == 2
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).eq(RemoteVtepVO_.clusterUuid,cluster2.uuid).count() == 1 
        expect(AssertionError.class) {
            createVxlanPoolRemoteVtep {
                l2NetworkUuid = pool.uuid
                clusterUuid = cluster2.uuid
                remoteVtepIp = "1.1.1.1"
            }        
        }

        deleteVxlanPoolRemoteVtep {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster.uuid 
            remoteVtepIp = "1.1.1.1"
        }
        assert !Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).eq(RemoteVtepVO_.clusterUuid,cluster.uuid).isExists()
        assert Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).count() == 1

        deleteVxlanPoolRemoteVtep {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster2.uuid 
            remoteVtepIp = "1.1.1.1"
        }
        assert !Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, pool.uuid).isExists()

    }
}
