package org.zstack.test.integration.db.deadlock

import com.google.common.collect.Lists
import org.hibernate.exception.LockAcquisitionException
import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.DatabaseFacadeImpl
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vo.ResourceVO
import org.zstack.header.vo.ResourceVO_
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

import javax.persistence.LockTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
/**
 * Created by lining on 2020/04/09.
 */
class BatchCreateVmFailDeadlockCase extends SubCase{
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "iso"
                    url  = "http://zstack.org/download/test.iso"
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
                        totalCpu = 1000
                        totalMem = SizeUnit.GIGABYTE.toByte(10000)
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100000)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(10000)
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.0.2"
                            endIp = "192.168.255.0"
                            netmask = "255.255.0.0"
                            gateway = "192.168.0.1"
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
            long startTime = new Date().getTime()

            dbf = bean(DatabaseFacade.class)

            String num = System.getProperty("resourceVONum")
            int numberOfResources = num == null ? 3000 : Integer.parseInt(num)
            List<ResourceVO> mockResourceVOs = persistResourceVOs(numberOfResources)

            String runTimes = System.getProperty("runTimes")
            int times = runTimes == null ? 1 : Integer.parseInt(runTimes)
            for (int i = 0; i < times; i++) {
                long s = new Date().getTime()
                testBatchCreateVm()
                long e = new Date().getTime()
                logger.warn("testBatchCreateVm-${i} execute time is ${e - s}")
            }

            List<String> mockResourceUuids = mockResourceVOs.stream().map({vo -> vo.getUuid()}).collect(Collectors.toList())
            List resourceUuidParts = Lists.partition(mockResourceUuids, 1000)
            resourceUuidParts.stream().forEach({ List resourceUuids ->
                SQL.New(ResourceVO.class)
                        .in(ResourceVO_.uuid, resourceUuids)
                        .delete()
            })

            long endTime = new Date().getTime()
            logger.warn("env.create execute time is ${endTime - startTime}")
        }
    }

    void testBatchCreateVm() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ImageInventory image = env.inventoryByName("iso")

        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) {FlatDhcpBackend.ApplyDhcpRsp rsp, HttpEntity<java.lang.String> e ->
            Random r = new Random()
            int ran = r.nextInt(2)
            if (ran == 0) {
                rsp.setError("case mock error")
            }

            return rsp
        }

        AtomicInteger count = new AtomicInteger(0)
        boolean dbDeadlockOccurred = false
        def threads = []
        String num = System.getProperty("vmNum")
        int vmNum = num == null ? 100 : Integer.parseInt(num)
        for (int i = 0; i < vmNum; i++) {
            def thread = Thread.start {
                CreateVmInstanceAction action = new CreateVmInstanceAction(
                        name : "test-" + i,
                        cpuNum: 1,
                        memorySize: gb(1),
                        l3NetworkUuids : [l3.uuid],
                        imageUuid : image.uuid,
                        sessionId: Test.currentEnvSpec.session.uuid,
                        diskAOs: [
                            [
                                "boot" : true,
                                "size" : gb(1),
                            ],
                            [
                                "size" : gb(1),
                            ]
                        ]
                )

                CreateVmInstanceAction.Result ret = action.call()
                if (ret.error != null) {
                    if (ret.error.getDetails().contains(LockAcquisitionException.class.simpleName)) {
                        dbDeadlockOccurred = true
                    } else if (ret.error.getDetails().contains(LockTimeoutException.class.simpleName)) {
                        dbDeadlockOccurred = true
                    }
                }
                count.incrementAndGet()
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        retryInSecs(vmNum, 3) {
            assert count.get() >= vmNum
        }
        assert !dbDeadlockOccurred

        List<String> vmUuids = Q.New(VmInstanceVO.class)
            .select(VmInstanceVO_.uuid)
            .listValues()
        for (String vmUuid : vmUuids) {
            destroyVmInstance {
                uuid = vmUuid
            }
        }

        assert DatabaseFacadeImpl.getDberror().longValue() > 0
    }

    private List<ResourceVO> persistResourceVOs(int num) {
        List<ResourceVO> mockResourceVOs = []
        for (int i = 0 ; i < num; i++) {
            String resourceUuid = Platform.getUuid()
            List<String> args = [resourceUuid, "mock by case", "Mock"]
            ResourceVO resourceVO = new ResourceVO(args.toArray())
            resourceVO.setConcreteResourceType("Mock")
            mockResourceVOs.add(resourceVO)
        }

        List resourceVOParts = Lists.partition(mockResourceVOs, 5000)
        resourceVOParts.stream().forEach({ List resourceVOS ->
            dbf.persistCollection(resourceVOS)
        })

        return mockResourceVOs
    }
}
