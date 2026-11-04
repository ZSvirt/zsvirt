package org.zstack.test.integration.allocator.domain

import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AllocatorDomainCascadeCase extends SubCase {
    EnvSpec env
    SessionInventory adminSession
    ZoneInventory zone
    ZoneInventory emptyZone

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
                        managementIp = "127.0.0.1"
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

            zone {
                name = "empty-zone"
                description = "no cluster"
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testDeleteAccountRemovesOwnedDomain()
            testDeleteZoneRemovesDomain()
        }
    }

    void prepare() {
        adminSession = env.session
        zone = env.inventoryByName("zone") as ZoneInventory
        emptyZone = env.inventoryByName("empty-zone") as ZoneInventory
    }

    void testDeleteAccountRemovesOwnedDomain() {
        logger.info("Test 001: create normal account, attach allocator.domain role, share zone, expect success")
        AccountInventory user = createAccount {
            delegate.name = "domain-owner"
            delegate.password = "password"
            delegate.sessionId = adminSession.uuid
        } as AccountInventory
        attachPredefineRoles(user.uuid, "allocator.domain")
        shareResource {
            delegate.resourceUuids = [zone.uuid]
            delegate.accountUuids = [user.uuid]
            delegate.sessionId = adminSession.uuid
        }
        SessionInventory userSession = logInByAccount {
            delegate.accountName = "domain-owner"
            delegate.password = "password"
        } as SessionInventory

        logger.info("Test 002: create allocator domain as the account, expect success")
        env.session = userSession
        AllocatorDomainInventory owned = createAllocatorDomain {
            delegate.name = "owned-domain"
            delegate.zoneUuid = zone.uuid
            delegate.sessionId = userSession.uuid
        } as AllocatorDomainInventory
        assert owned.uuid != null
        assert queryAllocatorDomain {
            delegate.conditions = ["uuid=${owned.uuid}"]
            delegate.sessionId = userSession.uuid
        }.size() == 1

        logger.info("Test 003: delete the owning account, expect the domain gone")
        env.session = adminSession
        deleteAccount {
            delegate.uuid = user.uuid
            delegate.sessionId = adminSession.uuid
        }
        assert queryAllocatorDomain {
            delegate.conditions = ["uuid=${owned.uuid}"]
            delegate.sessionId = adminSession.uuid
        }.isEmpty()
        assert queryAccount {
            delegate.conditions = ["uuid=${user.uuid}"]
            delegate.sessionId = adminSession.uuid
        }.isEmpty()
    }

    void testDeleteZoneRemovesDomain() {
        logger.info("Test 101: create allocator domain on empty zone, expect success")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "zone-domain"
            delegate.zoneUuid = emptyZone.uuid
            delegate.sessionId = adminSession.uuid
        } as AllocatorDomainInventory
        assert domain.uuid != null
        assert queryAllocatorDomain {
            delegate.conditions = ["uuid=${domain.uuid}"]
        }.size() == 1

        logger.info("Test 102: delete zone while domain still exists, expect zone and domain gone")
        deleteZone {
            delegate.uuid = emptyZone.uuid
            delegate.sessionId = adminSession.uuid
        }
        assert queryAllocatorDomain {
            delegate.conditions = ["uuid=${domain.uuid}"]
        }.isEmpty()
        assert queryZone {
            delegate.conditions = ["uuid=${emptyZone.uuid}"]
        }.isEmpty()
    }

    @Override
    void clean() {
        if (adminSession != null) {
            env.session = adminSession
        }
        env.delete()
    }
}
