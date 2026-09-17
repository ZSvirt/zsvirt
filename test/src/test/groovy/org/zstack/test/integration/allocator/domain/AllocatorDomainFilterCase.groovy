package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.entity.AllocatorDomainVO
import org.zstack.allocator.domain.header.AllocatorDomainState
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdk.allocator.domain.api.GetAllocatorDomainCandidatesResult
import org.zstack.sdk.allocator.domain.api.GetAllocatorDomainVmsResult
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AllocatorDomainFilterCase extends SubCase {
    EnvSpec env
    ZoneInventory zone
    HostInventory host
    ClusterInventory cluster
    PrimaryStorageInventory ps
    L3NetworkInventory l3

    @Override
    void setup() {
        useSpring(AllocatorDomainTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testGetCandidates()
            testGetVmsOnUnusedDomain()
        }
    }

    void prepare() {
        zone = env.inventoryByName("zone") as ZoneInventory
        host = env.inventoryByName("kvm") as HostInventory
        cluster = env.inventoryByName("cluster") as ClusterInventory
        ps = env.inventoryByName("local") as PrimaryStorageInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
    }

    void testGetCandidates() {
        logger.info("Test 001: get allocator domain candidates, expect the zone host/cluster/PS/L3")
        GetAllocatorDomainCandidatesResult result = getAllocatorDomainCandidates {
            delegate.zoneUuid = zone.uuid
        } as GetAllocatorDomainCandidatesResult
        assert result.hosts.size() == 1
        assert result.hosts[0].uuid == host.uuid
        assert result.clusters.size() == 1
        assert result.clusters[0].uuid == cluster.uuid
        assert result.primaryStorages.size() == 1
        assert result.primaryStorages[0].uuid == ps.uuid
        assert result.l3Networks.size() == 1
        assert result.l3Networks[0].uuid == l3.uuid
    }

    void testGetVmsOnUnusedDomain() {
        logger.info("Test 101: create unused allocator domain, expect success")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "unused-domain"
            delegate.zoneUuid = zone.uuid
        } as AllocatorDomainInventory
        assert domain.uuid != null
        assert domain.name == "unused-domain"
        assert domain.zoneUuid == zone.uuid
        assert domain.state == AllocatorDomainState.Enabled.toString()
        assert (domain.rules ?: []).isEmpty()
        assert (domain.scenes ?: []).isEmpty()

        AllocatorDomainVO vo = dbFindByUuid(domain.uuid, AllocatorDomainVO.class)
        assert vo != null
        assert vo.name == "unused-domain"
        assert vo.zoneUuid == zone.uuid
        assert vo.state == AllocatorDomainState.Enabled

        logger.info("Test 102: get vms of unused allocator domain, expect both lists empty")
        GetAllocatorDomainVmsResult result = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (result.currentVms ?: []).isEmpty()
        assert (result.nextStartVms ?: []).isEmpty()
        assert (result.candidateVms ?: []).isEmpty()

        logger.info("Test 199: delete unused allocator domain, expect it gone")
        deleteAllocatorDomain {
            delegate.uuid = domain.uuid
        }
        assert queryAllocatorDomain {
            delegate.conditions = ["uuid=${domain.uuid}"]
        }.isEmpty()
        assert dbFindByUuid(domain.uuid, AllocatorDomainVO.class) == null
    }

    @Override
    void clean() {
        queryAllocatorDomain {}.each {
            deleteAllocatorDomain {
                delegate.uuid = it.uuid
            }
        }
        env.delete()
    }
}
