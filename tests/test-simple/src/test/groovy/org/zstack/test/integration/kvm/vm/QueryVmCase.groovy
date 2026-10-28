package org.zstack.test.integration.kvm.vm

import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/5/4.
 */
class QueryVmCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
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

            vm {
                name = "vm"
                cpu = 1
                memoryGB(1)
                useImage("image1")
                useL3Networks("l3")
            }
            vm {
                name = "vm1"
                cpu = 1
                memoryGB(1)
                useImage("image1")
                useL3Networks("l3")
            }
            vm {
                name = "vm2"
                cpu = 1
                memoryGB(1)
                useImage("image1")
                useL3Networks("l3")
            }
            vm {
                name = "vm3"
                cpu = 1
                memoryGB(1)
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testQueryLimitAndStart()
            testQueryWithFields()
        }
    }

    void testQueryLimitAndStart() {
        VmInstanceInventory[] result = queryVmInstance {
            conditions=[]
            start = 0
            limit = 2
            sortBy = "name"
        }
        assert 2 == result.size()
        assert "vm" == result[0].name
        assert "vm1" == result[1].name

        result = queryVmInstance {
            conditions=[]
            start = 1
            limit = 3
            sortBy = "name"
        }
        assert 3 == result.size()
        assert "vm1" == result[0].name
        assert "vm2" == result[1].name
        assert "vm3" == result[2].name

        result = queryVmInstance {
            conditions=[]
            start = 2
            limit = 3
            sortBy = "name"
        }
        assert 2 == result.size()
        assert "vm2" == result[0].name
        assert "vm3" == result[1].name

        result = queryVmInstance {
            conditions = []
            limit = 1
            sortBy = "name"
            sortDirection = "desc"
        }

        assert 1 == result.size()
        assert "vm3" == result[0].name

        result = queryVmInstance {
            conditions = []
            limit = 1
            sortBy = "name"
            sortDirection = "asc"
        }

        assert 1 == result.size()
        assert "vm" == result[0].name
    }

    void testQueryWithFields() {
        logger.info("Test 011: query with vmNics")
        expectApiFailure({
            queryVmInstance {
                delegate.fields = ["uuid", "vmNics"]
            }
        }) {
            assert delegate.code == "SEARCH.2101"
            assert delegate.opaque["fields"] == ["vmNics"]
        }

        logger.info("Test 012: query with uuid and name")
        def results012 = queryVmInstance {
            delegate.fields = ["uuid", "name"]
        } as List
        assert results012.size() == 4

        logger.info("Test 013: query with unknown field")
        expectApiFailure({
            queryVmInstance {
                delegate.fields = ["notExists"]
            }
        }) {
            assert delegate.code == "SEARCH.2101"
            assert delegate.opaque["fields"] == ["notExists"]
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
