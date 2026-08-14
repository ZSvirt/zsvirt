package org.zstack.test.integration.network.vxlanNetwork

import org.springframework.http.HttpEntity
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.sdk.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.ZoneSpec

/**
 * @author: kefeng.wang
 * @date: 2018-12-12
 * */
class UpdateVniRangeCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachL2Network("l2")
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testUpdateVniRange()
        }
    }

    void testUpdateVniRange() {
        ZoneSpec zone = env.specByName("zone")
        ClusterSpec cluster = env.specByName("cluster1")

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            VxlanKvmAgentCommands.CheckVxlanCidrResponse resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse()
            resp.vtepIp = "127.0.0.1"
            resp.setSuccess(true)
            return resp
        }

        L2VxlanNetworkPoolInventory pool1 = createL2VxlanNetworkPool {
            name = "POOL-1"
            zoneUuid = zone.inventory.getUuid()
        }

        VniRangeInventory vniRangeOld = createVniRange {
            startVni = 100
            endVni = 200
            l2NetworkUuid = pool1.getUuid()
            name = "RANGE-OLD"
        }
        assert vniRangeOld.name == "RANGE-OLD"

        VniRangeInventory vniRangeNew = updateVniRange {
            uuid = vniRangeOld.uuid
            name = "RANGE-NEW"
        }
        assert vniRangeNew.name == "RANGE-NEW"

        attachL2NetworkToCluster {
            l2NetworkUuid = pool1.uuid
            clusterUuid = cluster.inventory.uuid
            systemTags = ["l2NetworkUuid::${pool1.getUuid()}::clusterUuid::${cluster.inventory.uuid}::cidr::{127.0.0.1/8}".toString()]
        }

        L2VxlanNetworkPoolInventory pool2 = createL2VxlanNetworkPool {
            name = "POOL-2"
            zoneUuid = zone.inventory.getUuid()
        }

        VniRangeInventory vniRange2 = createVniRange {
            startVni = 300
            endVni = 400
            l2NetworkUuid = pool2.getUuid()
            name = "RANGE-2"
        }

        VniRangeInventory vniRange3 = createVniRange {
            startVni = 100
            endVni = 200
            l2NetworkUuid = pool2.getUuid()
            name = "RANGE-3"
        }

        expect(AssertionError.class) {
            attachL2NetworkToCluster {
                l2NetworkUuid = pool2.uuid
                clusterUuid = cluster.inventory.uuid
                systemTags = ["l2NetworkUuid::${pool1.getUuid()}::clusterUuid::${cluster.inventory.uuid}::cidr::{127.0.0.1/8}".toString()]
            }
        }

        deleteVniRange {
            uuid = vniRange3.uuid
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = pool2.uuid
            clusterUuid = cluster.inventory.uuid
            systemTags = ["l2NetworkUuid::${pool1.getUuid()}::clusterUuid::${cluster.inventory.uuid}::cidr::{127.0.0.1/8}".toString()]
        }
        expect(AssertionError.class) {
            createVniRange {
                startVni = 100
                endVni = 200
                l2NetworkUuid = pool2.getUuid()
                name = "RANGE-3"
            }
        }

        assert queryVniRange {
            conditions=["l2NetworkUuid=${pool1.uuid}"]
        }.size() == 1

        assert queryVniRange {
            conditions=["vxlanPool.uuid=${pool1.uuid}"]
        }.size() == 1
    }

    @Override
    void clean() {
        env.delete()
    }
}
