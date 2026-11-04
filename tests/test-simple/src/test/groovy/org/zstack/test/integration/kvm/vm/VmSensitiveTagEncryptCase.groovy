package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmSensitiveTagEncryptor
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.header.vm.VmInstanceVO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.tag.SystemTagCreator
import org.zstack.tag.TagManager
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.TagUtils
import org.zstack.utils.data.SizeUnit

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

class VmSensitiveTagEncryptCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
        spring {
            include("encrypt.xml")
        }
    }

    @Override
    void environment() {
        env = Test.makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.MEGABYTE.toByte(512)
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
                    name = "image1"
                    architecture = "x86_64"
                    url = "http://zstack.org/download/test.qcow2"
                    virtio = true
                }

                image {
                    name = "vr"
                    architecture = "x86_64"
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

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

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
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            testCreatePlainVmSensitiveTags()
            testCreateEncryptedVm()
            testEnableEncryptionOnExistingVm()
            testDisableEncryptionOnExistingVm()
            testNonWhitelistTagNotDecrypted()
            testPlainVmKeepsEncPrefixedInput()
            testEncryptedVmEncryptsEncPrefixedInput()
            testCopySystemTagReencryptsForEncryptedTarget()
        }
    }

    private void destroyVm(String uuid) {
        destroyVmInstance { delegate.uuid = uuid }
        expungeVmInstance { delegate.uuid = uuid }
    }

    private static String rawTag(String vmUuid, String tagHead) {
        return Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, vmUuid)
                .eq(SystemTagVO_.resourceType, VmInstanceVO.class.simpleName)
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(String.format("%s::%%", tagHead)))
                .select(SystemTagVO_.tag)
                .findValue()
    }

    void testCreatePlainVmSensitiveTags() {
        def l3 = env.inventoryByName("l3")
        def password = "plain-console-pwd"
        VmInstanceInventory vm = createVmInstance {
            name = "plain-enc-vm"
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            imageUuid = env.inventoryByName("image1").uuid
            l3NetworkUuids = [l3.uuid]
            vmEncryption = false
        } as VmInstanceInventory

        setVmConsolePassword {
            uuid = vm.uuid
            consolePassword = password
        }

        String tag = rawTag(vm.uuid, VmSystemTags.CONSOLE_PASSWORD_TOKEN)
        assert tag != null
        assert !tag.contains(VmSensitiveTagEncryptor.ENC_PREFIX)
        assert !VmSystemTags.VM_ENCRYPTION.hasTag(vm.uuid)

        assert password == getVmConsolePassword {
            uuid = vm.uuid
        }.password

        destroyVm(vm.uuid)
    }

    void testCreateEncryptedVm() {
        def l3 = env.inventoryByName("l3")
        def password = "enc-console-pwd"
        def sshKey = "ssh-rsa AAAA"
        def userdata = Base64.encoder.encodeToString("cloud-init-data".bytes)

        VmInstanceInventory vm = createVmInstance {
            name = "encrypted-vm"
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            imageUuid = env.inventoryByName("image1").uuid
            l3NetworkUuids = [l3.uuid]
            vmEncryption = true
            systemTags = [
                    VmSystemTags.CONSOLE_PASSWORD.instantiateTag(map(e(VmSystemTags.CONSOLE_PASSWORD_TOKEN, password))),
                    VmSystemTags.SSHKEY.instantiateTag(map(e(VmSystemTags.SSHKEY_TOKEN, sshKey))),
                    VmSystemTags.USERDATA.instantiateTag(map(e(VmSystemTags.USERDATA_TOKEN, userdata))),
            ]
        } as VmInstanceInventory

        assert VmSystemTags.VM_ENCRYPTION.hasTag(vm.uuid)
        assert rawTag(vm.uuid, VmSystemTags.CONSOLE_PASSWORD_TOKEN).contains(VmSensitiveTagEncryptor.ENC_PREFIX)
        assert rawTag(vm.uuid, VmSystemTags.SSHKEY_TOKEN).contains(VmSensitiveTagEncryptor.ENC_PREFIX)
        assert rawTag(vm.uuid, VmSystemTags.USERDATA_TOKEN).contains(VmSensitiveTagEncryptor.ENC_PREFIX)

        assert password == getVmConsolePassword { uuid = vm.uuid }.password
        assert sshKey == getVmSshKey { uuid = vm.uuid }.sshKey
        assert userdata == VmSystemTags.USERDATA.getTokenByResourceUuid(vm.uuid, VmSystemTags.USERDATA_TOKEN)

        destroyVm(vm.uuid)
    }

    void testEnableEncryptionOnExistingVm() {
        def l3 = env.inventoryByName("l3")
        def password = "migrate-pwd"
        VmInstanceInventory vm = createVmInstance {
            name = "to-encrypt-vm"
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            imageUuid = env.inventoryByName("image1").uuid
            l3NetworkUuids = [l3.uuid]
        } as VmInstanceInventory

        setVmConsolePassword {
            uuid = vm.uuid
            consolePassword = password
        }
        assert !rawTag(vm.uuid, VmSystemTags.CONSOLE_PASSWORD_TOKEN).contains(VmSensitiveTagEncryptor.ENC_PREFIX)

        updateVmInstance {
            uuid = vm.uuid
            vmEncryption = true
        }

        retryInSecs {
            assert VmSystemTags.VM_ENCRYPTION.hasTag(vm.uuid)
            assert rawTag(vm.uuid, VmSystemTags.CONSOLE_PASSWORD_TOKEN).contains(VmSensitiveTagEncryptor.ENC_PREFIX)
            assert password == getVmConsolePassword { uuid = vm.uuid }.password
        }

        destroyVm(vm.uuid)
    }

    void testDisableEncryptionOnExistingVm() {
        def l3 = env.inventoryByName("l3")
        def password = "disable-enc-pwd"
        VmInstanceInventory vm = createVmInstance {
            name = "to-disable-enc-vm"
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            imageUuid = env.inventoryByName("image1").uuid
            l3NetworkUuids = [l3.uuid]
            vmEncryption = true
            systemTags = [
                    VmSystemTags.CONSOLE_PASSWORD.instantiateTag(map(e(VmSystemTags.CONSOLE_PASSWORD_TOKEN, password))),
            ]
        } as VmInstanceInventory

        assert VmSystemTags.VM_ENCRYPTION.hasTag(vm.uuid)
        assert rawTag(vm.uuid, VmSystemTags.CONSOLE_PASSWORD_TOKEN).contains(VmSensitiveTagEncryptor.ENC_PREFIX)

        updateVmInstance {
            uuid = vm.uuid
            vmEncryption = false
        }

        retryInSecs {
            assert !VmSystemTags.VM_ENCRYPTION.hasTag(vm.uuid)
            assert !rawTag(vm.uuid, VmSystemTags.CONSOLE_PASSWORD_TOKEN).contains(VmSensitiveTagEncryptor.ENC_PREFIX)
            assert password == getVmConsolePassword { uuid = vm.uuid }.password
        }

        destroyVm(vm.uuid)
    }

    void testNonWhitelistTagNotDecrypted() {
        def l3 = env.inventoryByName("l3")
        VmInstanceInventory vm = createVmInstance {
            name = "non-whitelist-vm"
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            imageUuid = env.inventoryByName("image1").uuid
            l3NetworkUuids = [l3.uuid]
        } as VmInstanceInventory

        String encValue = VmSensitiveTagEncryptor.ENC_PREFIX + "not-a-real-cipher"
        SystemTagCreator creator = VmSystemTags.VM_PRIORITY.newSystemTagCreator(vm.uuid)
        creator.setTagByTokens(map(e(VmSystemTags.VM_PRIORITY_TOKEN, encValue)))
        creator.recreate = true
        creator.create()

        assert encValue == VmSystemTags.VM_PRIORITY.getTokenByResourceUuid(vm.uuid, VmSystemTags.VM_PRIORITY_TOKEN)
        assert rawTag(vm.uuid, VmSystemTags.VM_PRIORITY_TOKEN) == VmSystemTags.VM_PRIORITY.instantiateTag(
                map(e(VmSystemTags.VM_PRIORITY_TOKEN, encValue)))

        destroyVm(vm.uuid)
    }

    void testPlainVmKeepsEncPrefixedInput() {
        def l3 = env.inventoryByName("l3")
        def plain = VmSensitiveTagEncryptor.ENC_PREFIX + "customer-input"

        VmInstanceInventory vm = createVmInstance {
            name = "plain-enc-prefix-vm"
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            imageUuid = env.inventoryByName("image1").uuid
            l3NetworkUuids = [l3.uuid]
            vmEncryption = false
            systemTags = [VmSystemTags.CONSOLE_PASSWORD.instantiateTag(map(e(VmSystemTags.CONSOLE_PASSWORD_TOKEN, plain)))]
        } as VmInstanceInventory

        assert !VmSystemTags.VM_ENCRYPTION.hasTag(vm.uuid)
        assert rawTag(vm.uuid, VmSystemTags.CONSOLE_PASSWORD_TOKEN) == "consolePassword::${plain}"
        assert plain == getVmConsolePassword { uuid = vm.uuid }.password

        destroyVm(vm.uuid)
    }

    void testEncryptedVmEncryptsEncPrefixedInput() {
        def l3 = env.inventoryByName("l3")
        def plain = VmSensitiveTagEncryptor.ENC_PREFIX + "customer-secret"

        VmInstanceInventory vm = createVmInstance {
            name = "encrypted-enc-prefix-vm"
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            imageUuid = env.inventoryByName("image1").uuid
            l3NetworkUuids = [l3.uuid]
            vmEncryption = true
            systemTags = [VmSystemTags.CONSOLE_PASSWORD.instantiateTag(map(e(VmSystemTags.CONSOLE_PASSWORD_TOKEN, plain)))]
        } as VmInstanceInventory

        assert VmSystemTags.VM_ENCRYPTION.hasTag(vm.uuid)
        assert rawTag(vm.uuid, VmSystemTags.CONSOLE_PASSWORD_TOKEN) != "consolePassword::${plain}"
        assert rawTag(vm.uuid, VmSystemTags.CONSOLE_PASSWORD_TOKEN).contains(VmSensitiveTagEncryptor.ENC_PREFIX)
        assert plain == getVmConsolePassword { uuid = vm.uuid }.password

        destroyVm(vm.uuid)
    }

    void testCopySystemTagReencryptsForEncryptedTarget() {
        def l3 = env.inventoryByName("l3")
        def plain = "copy-target-pwd"

        VmInstanceInventory src = createVmInstance {
            name = "copy-src-vm"
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            imageUuid = env.inventoryByName("image1").uuid
            l3NetworkUuids = [l3.uuid]
            vmEncryption = true
            systemTags = [VmSystemTags.CONSOLE_PASSWORD.instantiateTag(map(e(VmSystemTags.CONSOLE_PASSWORD_TOKEN, plain)))]
        } as VmInstanceInventory

        VmInstanceInventory dst = createVmInstance {
            name = "copy-dst-vm"
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            imageUuid = env.inventoryByName("image1").uuid
            l3NetworkUuids = [l3.uuid]
            vmEncryption = true
        } as VmInstanceInventory

        def tagMgr = bean(TagManager) as TagManager
        tagMgr.copySystemTag(src.uuid, VmInstanceVO.class.simpleName, dst.uuid, VmInstanceVO.class.simpleName, false)

        retryInSecs {
            assert rawTag(dst.uuid, VmSystemTags.CONSOLE_PASSWORD_TOKEN).contains(VmSensitiveTagEncryptor.ENC_PREFIX)
            assert plain == getVmConsolePassword { uuid = dst.uuid }.password
        }

        destroyVm(src.uuid)
        destroyVm(dst.uuid)
    }
}
