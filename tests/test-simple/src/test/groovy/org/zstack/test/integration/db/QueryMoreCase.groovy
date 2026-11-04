package org.zstack.test.integration.db

import org.zstack.core.db.Q
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.SimpleTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class QueryMoreCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    HostInventory host

    @Override
    void setup() {
        useSpring(SimpleTest.springSpec)
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
                    url  = "http://zstack.org/download/test.qcow2"
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

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

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
                cpu = 4
                memoryGB(8)
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testBasicQueryMode()
            testStartAndLimit()
        }
    }

    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory
        host = env.inventoryByName("kvm") as HostInventory
    }

    void testBasicQueryMode() {
        def q = Q.New(VmInstanceVO.class, HostVO.class)
                .table0()
                    .eq(VmInstanceVO_.uuid, vm.uuid)
                    .eq(VmInstanceVO_.hostUuid).table1(HostVO_.uuid)
                .table1()
                    .select(HostVO_.@name)
                    .orderByDesc(HostVO_.uuid)

        assert q.toString() == "select t1.name from VmInstanceVO t0,HostVO t1 where t0.uuid=:p0 and t0.hostUuid=t1.uuid order by t1.uuid DESC"
        def list = q.list()
        logger.info(list.toString())

        assert list.size() == 1
        assert list[0] == "kvm"

        def q2 = Q.New(HostVO.class)
                .select(HostVO_.@name)
                .in(HostVO_.uuid,
                        Q.New(VmInstanceVO.class)
                                .select(VmInstanceVO_.hostUuid)
                                .eq(VmInstanceVO_.uuid, vm.uuid)
                )
                .orderByDesc(HostVO_.uuid)

        assert q2.toString() == "select t0.name from HostVO t0 where t0.uuid in (select t1.hostUuid from VmInstanceVO t1 where t1.uuid=:p0) order by t0.uuid DESC"
        list = q2.list()
        logger.info(list.toString())

        assert list.size() == 1
        assert list[0] == "kvm"

        def q3 = Q.New(VmInstanceVO.class, AccountResourceRefVO.class)
                .table0()
                    .select(VmInstanceVO_.clusterUuid)
                    .eq(VmInstanceVO_.uuid).table1(AccountResourceRefVO_.resourceUuid)
                    .eq(VmInstanceVO_.type, "UserVm")
                .table1()
                    .eq(AccountResourceRefVO_.accountUuid, env.session.accountUuid)

        assert q3.toString() == "select t0.clusterUuid from VmInstanceVO t0,AccountResourceRefVO t1 where t0.type=:p0 and t1.accountUuid=:p1 and t0.uuid=t1.resourceUuid"
        list = q3.list()
        logger.info(list.toString())

        assert list.size() == 1
        assert list[0] == vm.clusterUuid

        def q4 = Q.New(VmInstanceVO.class, AccountResourceRefVO.class)
                .table0()
                    .selectThisTable()
                    .eq(VmInstanceVO_.uuid).table1(AccountResourceRefVO_.resourceUuid)
                    .eq(VmInstanceVO_.type, "UserVm")
                .table1()
                    .eq(AccountResourceRefVO_.accountUuid, env.session.accountUuid)

        assert q4.toString() == "select t0 from VmInstanceVO t0,AccountResourceRefVO t1 where t0.type=:p0 and t1.accountUuid=:p1 and t0.uuid=t1.resourceUuid"
        list = q4.list()
        logger.info(list.toString())

        assert list.size() == 1
        assert list[0] instanceof VmInstanceVO
        assert (list[0] as VmInstanceVO).uuid == vm.uuid

        def q5 = Q.New(VmInstanceVO.class, AccountResourceRefVO.class)
                .table0()
                    .selectCountThisTable()
                    .eq(VmInstanceVO_.uuid).table1(AccountResourceRefVO_.resourceUuid)
                    .eq(VmInstanceVO_.type, "UserVm")
                .table1()
                    .eq(AccountResourceRefVO_.accountUuid, env.session.accountUuid)

        assert q5.toString() == "select count(t0) from VmInstanceVO t0,AccountResourceRefVO t1 where t0.type=:p0 and t1.accountUuid=:p1 and t0.uuid=t1.resourceUuid"
        long count = q5.count()
        assert count == 1L

        def q6 = Q.New(VmInstanceVO.class, AccountResourceRefVO.class)
                .table0()
                    .selectThisTable()
                    .eq(VmInstanceVO_.uuid).table1(AccountResourceRefVO_.resourceUuid)
                    .in(VmInstanceVO_.type, ["UserVm", "ApplianceVm"])
                .table1()
                    .eq(AccountResourceRefVO_.accountUuid, env.session.accountUuid)

        assert q6.toString() == "select t0 from VmInstanceVO t0,AccountResourceRefVO t1 where t0.type in :p0 and t1.accountUuid=:p1 and t0.uuid=t1.resourceUuid"
        list = q6.list()
        logger.info(list.toString())

        assert list.size() == 1
        assert list.every { it instanceof VmInstanceVO }
        assert list.any { (it as VmInstanceVO).uuid == vm.uuid }
    }

    void testStartAndLimit() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def image = env.inventoryByName("image1") as ImageInventory
        (1..3).each { i ->
            createVmInstance {
                delegate.name = "page-vm-${i}"
                delegate.cpuNum = 1
                delegate.memorySize = SizeUnit.GIGABYTE.toByte(1)
                delegate.l3NetworkUuids = [l3.uuid]
                delegate.imageUuid = image.uuid
            }
        }

        List<String> all = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .orderByAsc(VmInstanceVO_.uuid)
                .toQueryMore()
                .list()
        assert all.size() >= 4

        logger.info("Test 101: Hibernate 5.3 HQL has no LIMIT or OFFSET production, so QueryMore must not write start/limit into the HQL string. Expect neither token")
        def paged = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .orderByAsc(VmInstanceVO_.uuid)
                .toQueryMore()
                .start(1)
                .limit(2)
        def hql = paged.toString()
        assert !hql.contains("limit")
        assert !hql.contains("offset")
        assert hql == "select t0.uuid from VmInstanceVO t0 order by t0.uuid ASC"

        logger.info("Test 102: start=1 and limit=2 are applied by setFirstResult/setMaxResults, expect the 2nd and 3rd uuid")
        List<String> page = paged.list()
        assert page.size() == 2
        assert page == all.subList(1, 3)

        logger.info("Test 103: limit=2 without start, expect the first 2 uuids")
        List<String> firstPage = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .orderByAsc(VmInstanceVO_.uuid)
                .toQueryMore()
                .limit(2)
                .list()
        assert firstPage == all.subList(0, 2)

        logger.info("Test 104: start=1 without limit, expect every uuid except the first")
        List<String> skipped = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .orderByAsc(VmInstanceVO_.uuid)
                .toQueryMore()
                .start(1)
                .list()
        assert skipped == all.subList(1, all.size())
    }

    @Override
    void clean() {
        env.delete()
    }
}
