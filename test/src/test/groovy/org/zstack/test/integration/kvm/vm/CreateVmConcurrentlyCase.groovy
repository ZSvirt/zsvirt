package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmQuotaConstant
import org.zstack.core.db.Q
import org.zstack.header.identity.AccountType
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.image.ImageQuotaConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.stopwatch.StopWatch

import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

///**
// * Created by kayo on 2018/3/20.
// */
class CreateVmConcurrentlyCase extends SubCase {
    EnvSpec env

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
        //CloudBusGlobalProperty.HTTP_ALWAYS = true

        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
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
                        name = "host1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(1000)
                        totalCpu = 1000
                    }

                    kvm {
                        name = "host2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(1000)
                        totalCpu = 1000
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                    attachL2Network("l2-2")
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
                            startIp = "192.168.0.2"
                            endIp = "192.168.255.254"
                            netmask = "255.255.0.0"
                            gateway = "192.168.0.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "l3-vr"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.0.2"
                            endIp = "192.168.255.254"
                            netmask = "255.255.0.0"
                            gateway = "192.168.0.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "12.16.0.2"
                            endIp = "12.16.255.254"
                            netmask = "255.255.0.0"
                            gateway = "12.16.0.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("host1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            def flatl3 = env.inventoryByName("l3") as L3NetworkInventory
            def vrl3 = env.inventoryByName("l3-vr") as L3NetworkInventory
            def host = env.inventoryByName("host1") as HostInventory
            def l3uuid = System.getProperty("useVR") == null ? flatl3.uuid : vrl3.uuid

            def startCreateHosts = System.currentTimeMillis()
            String newHosts = System.getProperty("newHosts")
            int num = 0
            if (newHosts != null) {
                num = Integer.parseInt(newHosts)
            }
            prepareNewHosts(host.clusterUuid, num)
            logger.debug(String.format("create ${num} Hosts spends: %s ms", System.currentTimeMillis() - startCreateHosts))

            StopWatch sw = Utils.getStopWatch()
            List<String> vmUuids
            int n = 50
            try {
                String numberOfVM = System.getProperty("numberOfVM")
                if (numberOfVM != null) {
                    n = Integer.parseInt(numberOfVM)
                }

                sw.start()
                vmUuids = testCreateVMConcurrently(n, l3uuid)
            } finally {
                sw.stop()
                logger.info(String.format("XXX: Creating %d VMs costs %d seconds", n, sw.getLapse(TimeUnit.SECONDS)))
            }

            if (System.getProperty("testStartVM") != null && vmUuids != null) {
                testStartVM(vmUuids)
            }

            testCreateVMWithQuota()
        }
    }

    void prepareNewHosts(String clusterUuid, int num) {
        int nhost = num
        def count = new AtomicInteger(0)
        logger.info(String.format("XXX: additional host: %d", nhost))
        int c = 0
        def s1 = System.currentTimeMillis()
        def s2 = System.currentTimeMillis()
        for (int i = 0; i < nhost; i++) {
            int idx = (i + 2 + 1) % 256
            if (idx == 0) {
                c += 1
            }
            new AddKVMHostAction(
                    name: "host" + idx,
                    managementIp: "127.0.$c.$idx",
                    username: "root",
                    password: "password",
                    clusterUuid: clusterUuid,
                    sessionId: adminSession(),
            ).call(new Completion<AddKVMHostAction.Result>() {
                @Override
                void complete(AddKVMHostAction.Result ret) {
                    count.incrementAndGet()
                    logger.info(String.format("add host ${count.intValue()} spend: %s ms", System.currentTimeMillis() - s1))
                    logger.info(String.format("add host ${count.intValue()} spend from last time: %s ms", System.currentTimeMillis() - s2))
                    s2 = System.currentTimeMillis()
                }
            })
        }

        while (count.get() < nhost) {
            TimeUnit.SECONDS.sleep(1)
        }
    }

    void testCreateVMWithQuota() {
        def existingVM = Q.New(VmInstanceVO.class).count()
        def runningVmQuota = 4
        def imageNumQuota = 1

        def userpass = "password"
        def newAccount = createAccount {
            name = "normaluser1"
            password = userpass
            type = AccountType.Normal.toString()
        } as AccountInventory
        attachPredefineRoles(newAccount.uuid, "vm", "image")

        String vmName = "test-vm-quota"
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = env.inventoryByName("image") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        shareResource {
            resourceUuids = [instanceOffering.uuid, image.uuid, l3.uuid]
            accountUuids = [newAccount.uuid]
        }

        updateQuota {
            identityUuid = newAccount.uuid
            name = VmQuotaConstant.VM_TOTAL_NUM
            value = runningVmQuota + 1
        }

        updateQuota {
            identityUuid = newAccount.uuid
            name = VmQuotaConstant.VM_RUNNING_NUM
            value = runningVmQuota
        }

        updateQuota {
            identityUuid = newAccount.uuid
            name = ImageQuotaConstant.IMAGE_NUM
            value = imageNumQuota
        }

        def list = []

        SessionInventory userSessionInv = logInByAccount {
            accountName = newAccount.name
            password = userpass
        } as SessionInventory

        for (int i = 0; i < runningVmQuota + 1; i++) {
            def thread = Thread.start {
                try {
                    createVmInstance {
                        name = vmName
                        instanceOfferingUuid = instanceOffering.uuid
                        imageUuid = image.uuid
                        l3NetworkUuids = [l3.uuid]
                        sessionId = userSessionInv.uuid
                    } as VmInstanceInventory
                } catch (AssertionError ignored) {
                }
            }

            list.add(thread)
        }

        list.each { it.join() }

        assert Q.New(VmInstanceVO.class).count() == existingVM + runningVmQuota
        def vmUuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.name, vmName).limit(1).select(VmInstanceVO_.uuid).listValues().get(0) as String
        stopVmInstance {
            uuid = vmUuid
            sessionId = userSessionInv.uuid
        }

        createVmInstance {
            name = vmName
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            sessionId = userSessionInv.uuid
        }

        def hasError = false
        try {
            startVmInstance {
                uuid = vmUuid
            }
        } catch (AssertionError ignored) {
            assert ignored.toString().contains(VmQuotaConstant.VM_RUNNING_NUM)
            hasError = true
        }

        assert hasError

        // stop all running VMs of normaluser1
        List<VmInstanceInventory> vms = queryVmInstance {
            sessionId = userSessionInv.uuid
        } as List<VmInstanceInventory>

        vms.each {
            def curUuid = it.uuid
            if (it.state == VmInstanceState.Running.toString()) {
                stopVmInstance {
                    uuid = curUuid
                }
            }
        }

        list.clear()
        AtomicInteger successCount = new AtomicInteger()
        vms.each {
            def thisUuid = it.uuid
            def thread = Thread.start {
                try {
                    startVmInstance {
                        uuid = thisUuid
                        sessionId = userSessionInv.uuid
                    }

                    successCount.incrementAndGet()
                } catch (AssertionError ignored) {
                }
            }

            list.add(thread)
        }

        list.each { it.join() }

        assert successCount.get() == runningVmQuota

        stopVmInstance {
            uuid = vms.get(0).getUuid()
        }

        def cnt = 0
        hasError = false
        for (int i = 0; i < imageNumQuota + 1; ++i) {
            try {
                createRootVolumeTemplateFromRootVolume {
                    name = "vm-template"
                    rootVolumeUuid = vms.get(0).getRootVolumeUuid()
                    sessionId = userSessionInv.uuid
                }
                cnt += 1
            } catch (AssertionError ignored) {
                hasError = true
            }
        }

        assert hasError
        assert cnt == imageNumQuota

        hasError = false
        try {
            createDataVolumeTemplateFromVolume {
                name = "data-template"
                volumeUuid = vms.get(0).getRootVolumeUuid()
                sessionId = userSessionInv.uuid
            }
        } catch (AssertionError ignored) {
            hasError = true
        }

        assert hasError
    }

    // This case is for ZSTAC-8576
    // PR system will met API timeout (api timeout is 25s)
    // Can be execute separately if needed
    List<String> testCreateVMConcurrently(int numberOfVM, String l3Uuid) {
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = env.inventoryByName("image") as ImageInventory

        def vmUuids = new ConcurrentSkipListSet()
        def existingVM = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Running).count()
        def count = new AtomicInteger(0)

        logger.info("XXX: creating $numberOfVM VMs ...")
        for (int i = 0; i < numberOfVM; i++) {
            new CreateVmInstanceAction(
                    name: "test-vm",
                    instanceOfferingUuid: instanceOffering.uuid,
                    imageUuid: image.uuid,
                    l3NetworkUuids: [l3Uuid],
                    systemTags: ["createWithoutCdRom::true"],
                    sessionId: adminSession(),
            ).call(new Completion<CreateVmInstanceAction.Result>() {
                @Override
                void complete(CreateVmInstanceAction.Result ret) {
                    count.incrementAndGet()
                    if (ret.error == null) {
                        def uuid = ret.value.inventory.uuid
                        vmUuids.add(uuid)
                        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, uuid).eq(VmInstanceVO_.state, VmInstanceState.Running).isExists()
                    }
                }
            })
        }

        while (count.get() < numberOfVM) {
            TimeUnit.SECONDS.sleep(1)
        }

        logger.info("XXX: created $numberOfVM VMs ...")
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.type, "UserVm").eq(VmInstanceVO_.state, VmInstanceState.Running).count() == existingVM + numberOfVM

        return vmUuids.toList()
    }

    void testStartVM(List<String> vmUuids) {
        def count = new AtomicInteger(0)
        def numberOfVM = vmUuids.size()
        logger.info("XXX: stopping $numberOfVM VMs ...")
        for (String vmUuid: vmUuids) {
            new StopVmInstanceAction(
                    uuid: vmUuid,
                    sessionId: adminSession(),
            ).call(new Completion<StopVmInstanceAction.Result>() {
                @Override
                void complete(StopVmInstanceAction.Result ret) {
                    count.incrementAndGet()
                }
            })
        }

        while (count.get() < numberOfVM) {
            TimeUnit.SECONDS.sleep(1)
        }

        assert Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.type, "UserVm")
                .eq(VmInstanceVO_.state, VmInstanceState.Stopped)
                .in(VmInstanceVO_.uuid, vmUuids)
                .count().intValue() == vmUuids.size()
        logger.info("XXX: stopped $numberOfVM VMs ...")
        logger.info("XXX: starting $numberOfVM VMs ...")
        count.set(0)

        StopWatch sw = Utils.getStopWatch()
        sw.start()
        for (String vmUuid: vmUuids) {
            new StartVmInstanceAction(
                    uuid: vmUuid,
                    sessionId: adminSession(),
            ).call(new Completion<StartVmInstanceAction.Result>() {
                @Override
                void complete(StartVmInstanceAction.Result ret) {
                    count.incrementAndGet()
                }
            })
        }

        while (count.get() < numberOfVM) {
            TimeUnit.SECONDS.sleep(1)
        }
        sw.stop()

        assert Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.type, "UserVm")
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .in(VmInstanceVO_.uuid, vmUuids)
                .count().intValue() == vmUuids.size()
        logger.info("XXX: started $numberOfVM VMs ...")
        logger.info(String.format("XXX: Start $numberOfVM VMs costs %d seconds", sw.getLapse(TimeUnit.SECONDS)))
    }
}
