package org.zstack.test.integration.kvm.host

import org.zstack.core.convert.PasswordConverter
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.encrypt.EncryptFacade
import org.zstack.core.encrypt.EncryptFacadeImpl
import org.zstack.core.encrypt.EncryptGlobalConfig
import org.zstack.header.core.encrypt.EncryptEntityMetadataVO
import org.zstack.header.core.encrypt.EncryptEntityMetadataVO_
import org.zstack.header.core.encrypt.EncryptEntityState
import org.zstack.kvm.KVMHostVO
import org.zstack.kvm.KVMHostVO_
import org.zstack.sdk.HostInventory
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.BeanUtils

import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Table

/**
 * Created by kayo on 2018/9/12.
 */
class HostPasswordEncryptCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
        spring {
            include("encrypt.xml")
        }
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"

                cluster {
                    name = "cluster"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }
                }
            }
        }
    }

    void testHostPasswordEncrypt() {
        EncryptFacade encryptFacade = bean(EncryptFacade)
        def host = env.inventoryByName("kvm1") as HostInventory

        def password = Q.New(KVMHostVO.class).select(KVMHostVO_.password).eq(KVMHostVO_.uuid, host.uuid).findValue()

        assert password == "password"

        BeanUtils.reflections.getFieldsAnnotatedWith(Convert.class).forEach({ ec ->
            if (ec.getDeclaringClass().getAnnotation(Entity.class) == null || ec.getDeclaringClass().getAnnotation(Table.class) == null) {
                return;
            }
            if (!ec.getAnnotation(Convert.class).converter().equals(PasswordConverter.class)) {
                return;
            }

            String entityName = ec.getDeclaringClass().getSimpleName();
            String columnName = ec.getName();
            assert Q.New(EncryptEntityMetadataVO.class).select(EncryptEntityMetadataVO_.state).eq(EncryptEntityMetadataVO_.entityName, entityName).eq(EncryptEntityMetadataVO_.columnName, columnName).findValue() == EncryptEntityState.NewAdded
        })

        updateGlobalConfig {
            category = EncryptGlobalConfig.CATEGORY
            name = EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.name
            value = "LocalEncryption"
        }

        retryInSecs {
            password = Q.New(KVMHostVO.class).select(KVMHostVO_.password).eq(KVMHostVO_.uuid, host.uuid).findValue()

            assert password == "password"
        }

        BeanUtils.reflections.getFieldsAnnotatedWith(Convert.class).forEach({ ec ->
            if (ec.getDeclaringClass().getAnnotation(Entity.class) == null || ec.getDeclaringClass().getAnnotation(Table.class) == null) {
                return;
            }
            if (!ec.getAnnotation(Convert.class).converter().equals(PasswordConverter.class)) {
                return;
            }

            String entityName = ec.getDeclaringClass().getSimpleName();
            String columnName = ec.getName();
            retryInSecs {
                assert Q.New(EncryptEntityMetadataVO.class).select(EncryptEntityMetadataVO_.state).eq(EncryptEntityMetadataVO_.entityName, entityName).eq(EncryptEntityMetadataVO_.columnName, columnName).findValue() == EncryptEntityState.Encrypted
            }
        })

        assert Q.New(EncryptEntityMetadataVO.class).select(EncryptEntityMetadataVO_.state).eq(EncryptEntityMetadataVO_.entityName, KVMHostVO.class.getSimpleName()).findValue() == EncryptEntityState.Encrypted

        //  test for unencrypted strings Decryption failures
        SQL.New(EncryptEntityMetadataVO.class)
                .eq(EncryptEntityMetadataVO_.entityName, KVMHostVO.class.getSimpleName())
                .set(EncryptEntityMetadataVO_.state, EncryptEntityState.NewAdded)
                .update()

        ((EncryptFacadeImpl) encryptFacade).handleNewAddedEncryptEntity()

        assert Q.New(EncryptEntityMetadataVO.class).select(EncryptEntityMetadataVO_.state).eq(EncryptEntityMetadataVO_.entityName, KVMHostVO.class.getSimpleName()).findValue() == EncryptEntityState.Encrypted

        retryInSecs {
            password = Q.New(KVMHostVO.class).select(KVMHostVO_.password).eq(KVMHostVO_.uuid, host.uuid).findValue()

            // the value of findValue query will be processed by convertToEntityAttribute
            assert password == "password"
        }

        //  test handleNewAddedEncryptEntity again, result unchanged
        SQL.New(EncryptEntityMetadataVO.class)
                .eq(EncryptEntityMetadataVO_.entityName, KVMHostVO.class.getSimpleName())
                .set(EncryptEntityMetadataVO_.state, EncryptEntityState.NewAdded)
                .update()

        ((EncryptFacadeImpl) encryptFacade).handleNewAddedEncryptEntity()

        assert Q.New(EncryptEntityMetadataVO.class).select(EncryptEntityMetadataVO_.state).eq(EncryptEntityMetadataVO_.entityName, KVMHostVO.class.getSimpleName()).findValue() == EncryptEntityState.Encrypted

        retryInSecs {
            password = Q.New(KVMHostVO.class).select(KVMHostVO_.password).eq(KVMHostVO_.uuid, host.uuid).findValue()

            // the value of findValue query will be processed by convertToEntityAttribute
            assert password == "password"
        }

        List<Tuple> needToChangeHosts = Q.New(KVMHostVO.class).select(KVMHostVO_.password, KVMHostVO_.uuid).listTuple()
        needToChangeHosts.forEach({ changeHost ->
            SQL.New(KVMHostVO.class)
                    .eq(KVMHostVO_.uuid, changeHost.get(1, String.class))
                    .set(KVMHostVO_.password, encryptFacade.decrypt(changeHost.get(0, String.class)))
                    .update()
        })

        retryInSecs {
            password = Q.New(KVMHostVO.class).select(KVMHostVO_.password).eq(KVMHostVO_.uuid, host.uuid).findValue()

            assert password == "password"
        }

        updateGlobalConfig {
            category = EncryptGlobalConfig.CATEGORY
            name = EncryptGlobalConfig.ENABLE_PASSWORD_ENCRYPT.name
            value = "None"
        }

        retryInSecs {
            password = Q.New(KVMHostVO.class).select(KVMHostVO_.password).eq(KVMHostVO_.uuid, host.uuid).findValue()

            assert password == "password"
        }

        BeanUtils.reflections.getFieldsAnnotatedWith(Convert.class).forEach({ ec ->
            if (ec.getDeclaringClass().getAnnotation(Entity.class) == null || ec.getDeclaringClass().getAnnotation(Table.class) == null) {
                return;
            }
            if (!ec.getAnnotation(Convert.class).converter().equals(PasswordConverter.class)) {
                return;
            }

            String entityName = ec.getDeclaringClass().getSimpleName();
            String columnName = ec.getName();
            assert Q.New(EncryptEntityMetadataVO.class).select(EncryptEntityMetadataVO_.state).eq(EncryptEntityMetadataVO_.entityName, entityName).eq(EncryptEntityMetadataVO_.columnName, columnName).findValue() == EncryptEntityState.NewAdded

        })

        assert Q.New(EncryptEntityMetadataVO.class).select(EncryptEntityMetadataVO_.state).eq(EncryptEntityMetadataVO_.entityName, KVMHostVO.class.getSimpleName()).findValue() == EncryptEntityState.NewAdded

        ((EncryptFacadeImpl) encryptFacade).handleNewAddedEncryptEntity()

        assert Q.New(EncryptEntityMetadataVO.class).select(EncryptEntityMetadataVO_.state).eq(EncryptEntityMetadataVO_.entityName, KVMHostVO.class.getSimpleName()).findValue() == EncryptEntityState.NewAdded
        assert Q.New(EncryptEntityMetadataVO.class).select(EncryptEntityMetadataVO_.state).eq(EncryptEntityMetadataVO_.entityName, CephBackupStorageMonVO.class.getSimpleName()).findValue() == EncryptEntityState.NewAdded
        assert Q.New(EncryptEntityMetadataVO.class).select(EncryptEntityMetadataVO_.state).eq(EncryptEntityMetadataVO_.entityName, CephPrimaryStorageMonVO.class.getSimpleName()).findValue() == EncryptEntityState.NewAdded

        retryInSecs {
            password = Q.New(KVMHostVO.class).select(KVMHostVO_.password).eq(KVMHostVO_.uuid, host.uuid).findValue()

            assert password == "password"
        }
    }


    @Override
    void test() {
        env.create {
            testHostPasswordEncrypt()
        }
    }
}
