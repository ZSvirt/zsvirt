package org.zstack.test.integration.db.schema

import org.zstack.core.db.Q
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.identity.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class QueryMoreCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    HostInventory host

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testBasicQueryMode()
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

    @Override
    void clean() {
        env.delete()
    }
}
