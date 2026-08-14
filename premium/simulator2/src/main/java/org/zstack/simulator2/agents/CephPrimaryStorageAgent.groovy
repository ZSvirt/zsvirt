package org.zstack.simulator2.agents

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.kvm.KVMAgentCommands
import org.zstack.simulator2.Simulator
import org.zstack.simulator2.SimulatorGlobalProperty
import org.zstack.simulator2.config.primaryStorage.CephPrimaryStorage
import org.zstack.simulator2.config.primaryStorage.CephPrimaryStorageMon
import org.zstack.simulator2.config.primaryStorage.CephPrimaryStoragePool
import org.zstack.storage.ceph.CephConstants
import org.zstack.storage.ceph.CephGlobalProperty
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO_

/**
 * Created by xing5 on 2017/9/19.
 */
class CephPrimaryStorageAgent extends Agent {
    CephPrimaryStorageAgent(Simulator simulator) {
        super(simulator)
        CephGlobalProperty.PRIMARY_STORAGE_AGENT_PORT = SimulatorGlobalProperty.SIMULATOR_AGENT_PORT
    }

    private CephPrimaryStorage find(HttpEntity<String> e) {
        String monIp = e.getHeaders().getFirst(Simulator.REMOTE_ADDR)
        assert monIp != null

        def ceph = CephPrimaryStorage.class.simpleName
        def mon = CephPrimaryStorageMon.class.simpleName

        def ret = simulator.sqlite.find("select ${ceph}.* from ${ceph} inner join ${mon} where ${mon}.cephId = ${ceph}.id and ${mon}.ip = '${monIp}'", CephPrimaryStorage.class)
        assert ret != null : "CEPH primary storage[mon ip: ${monIp}] has no simulator"
        return ret
    }


    @Override
    void setupAgentHandler() {
        handle(CephPrimaryStorageBase.GET_FACTS) { HttpEntity<String> e ->
            CephPrimaryStorageBase.GetFactsCmd cmd = json(e, CephPrimaryStorageBase.GetFactsCmd.class)

            CephPrimaryStorageMonVO monvo = Q.New(CephPrimaryStorageMonVO.class).eq(CephPrimaryStorageMonVO_.uuid, cmd.monUuid).find()
            assert monvo != null : "cannot find mon[uuid:${monvo.uuid}] in database"
            CephPrimaryStorageMon mon = simulator.sqlite.find("select * from ${CephPrimaryStorageMon.class.simpleName} where ip = '${monvo.hostname}'", CephPrimaryStorageMon.class)
            assert mon != null : "ceph primary storage mon[ip:${monvo.hostname} has no simulator"
            CephPrimaryStorage ps = simulator.sqlite.find("select * from ${CephPrimaryStorage.class.simpleName} where id = '${mon.cephId}'", CephPrimaryStorage.class)
            assert ps != null

            def rsp = new CephPrimaryStorageBase.GetFactsRsp()
            rsp.fsid = ps.fsid
            rsp.monAddr = mon.monAddr

            return rsp
        }

        handle(CephPrimaryStorageBase.DELETE_POOL_PATH) {
            return new CephPrimaryStorageBase.DeletePoolRsp()
        }

        handle(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e ->
            def ceph = find(e)

            def cmd = json(e.body, CephPrimaryStorageBase.InitCmd.class)
            if (cmd.pools != null) {
                cmd.pools.each { CephPrimaryStorageBase.Pool pool ->
                    boolean has = simulator.sqlite.findById(pool.name, CephPrimaryStoragePool.class) != null
                    if (!has && pool.predefined) {
                        throw new Exception("no pool[name:${pool.name}] found")
                    }

                    if (!has) {
                        simulator.sqlite.persist(new CephPrimaryStoragePool(id:pool.name, name: pool.name, cephId: ceph.id))
                    }
                }
            }

            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = ceph.fsid
            rsp.userKey = Platform.uuid
            rsp.totalCapacity = ceph.totalCapacity
            rsp.availableCapacity = ceph.availableCapacity
            if (cmd.pools != null) {
                List<CephPoolCapacity> poolCapacities = []

                long capacity =  ceph.totalCapacity / cmd.pools.size()
                cmd.pools.each { CephPrimaryStorageBase.Pool pool ->
                    poolCapacities.add(new CephPoolCapacity(
                            name : pool.name,
                            availableCapacity : capacity,
                            usedCapacity : 0,
                            totalCapacity : capacity,
                            relatedOsds: 'osd.1'
                    ))
                }

                rsp.poolCapacities = poolCapacities
                rsp.type = CephConstants.CEPH_MANUFACTURER_OPENSOURCE
            }

            return rsp
        }

        handle(CephPrimaryStorageBase.CHECK_POOL_PATH) { HttpEntity<String> e ->
            def cmd = json(e, CephPrimaryStorageBase.CheckCmd.class)
            def ceph = find(e)

            cmd.pools.each { pool ->
                if (simulator.sqlite.find("select * from ${CephPrimaryStoragePool.class.simpleName} where cephId = '${ceph.id}' and name = '${pool.name}'", CephPrimaryStoragePool.class) == null) {
                    throw new Exception("no pool[name:${pool.name}] found")
                }
            }

            def rsp = new CephPrimaryStorageBase.CheckRsp()
            rsp.success = true
            return rsp
        }

        handle(CephPrimaryStorageBase.CREATE_VOLUME_PATH) {
            return new CephPrimaryStorageBase.CreateEmptyVolumeRsp()
        }

        handle(CephPrimaryStorageBase.KVM_CREATE_SECRET_PATH) {
            return new  KVMAgentCommands.AgentResponse()
        }

        handle(CephPrimaryStorageBase.DELETE_PATH) {
            return new CephPrimaryStorageBase.DeleteRsp()
        }

        handle(CephPrimaryStorageMonBase.ECHO_PATH) { HttpEntity<String> entity ->
            return [:]
        }

        handle(CephPrimaryStorageBase.CREATE_SNAPSHOT_PATH) { HttpEntity<String> e ->
            def cmd = json(e, CephPrimaryStorageBase.CreateSnapshotCmd.class)
            def rsp = new CephPrimaryStorageBase.CreateSnapshotRsp()
            rsp.size = 0
            rsp.installPath = cmd.snapshotPath
            return rsp
        }

        handle(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) {
            return new CephPrimaryStorageBase.DeleteSnapshotRsp()
        }

        handle(CephPrimaryStorageBase.PURGE_SNAPSHOT_PATH) {
            return new CephPrimaryStorageBase.PurgeSnapshotRsp()
        }

        handle(CephPrimaryStorageBase.PROTECT_SNAPSHOT_PATH) {
            return new CephPrimaryStorageBase.ProtectSnapshotRsp()
        }

        handle(CephPrimaryStorageBase.UNPROTECT_SNAPSHOT_PATH) {
            return new CephPrimaryStorageBase.UnprotectedSnapshotRsp()
        }

        handle(CephPrimaryStorageBase.CLONE_PATH) {
            return new CephPrimaryStorageBase.CloneRsp()
        }

        handle(CephPrimaryStorageBase.FLATTEN_PATH) {
            return new CephPrimaryStorageBase.FlattenRsp()
        }

        handle(CephPrimaryStorageBase.CP_PATH) { HttpEntity<String> e ->
            def cmd = json(e, CephPrimaryStorageBase.CpCmd.class)
            def rsp = new CephPrimaryStorageBase.CpRsp()
            rsp.size = 0
            rsp.actualSize = 0
            rsp.installPath = cmd.dstPath
            return rsp
        }

        handle(CephPrimaryStorageBase.GET_VOLUME_SIZE_PATH) {
            def rsp = new CephPrimaryStorageBase.GetVolumeSizeRsp()
            rsp.actualSize = 0
            rsp.size = 0
            return rsp
        }

        handle(CephPrimaryStorageBase.ROLLBACK_SNAPSHOT_PATH) {
            return new CephPrimaryStorageBase.RollbackSnapshotRsp()
        }

        handle(CephPrimaryStorageBase.KVM_HA_SETUP_SELF_FENCER) {
            return new CephPrimaryStorageBase.AgentResponse()
        }

        handle(CephPrimaryStorageBase.KVM_HA_CANCEL_SELF_FENCER) {
            return new CephPrimaryStorageBase.AgentResponse()
        }

        handle(CephPrimaryStorageBase.DELETE_IMAGE_CACHE) {
            return new CephPrimaryStorageBase.AgentResponse()
        }

        handle(CephPrimaryStorageBase.ADD_POOL_PATH) { HttpEntity<String> e ->
            def ceph = find(e)
            def cmd = json(e.body, CephPrimaryStorageBase.AddPoolCmd.class)
            def rsp = new CephPrimaryStorageBase.AddPoolRsp()

            if (cmd.isCreate) {
                simulator.sqlite.persist(new CephPrimaryStoragePool(id: cmd.poolName, name: cmd.poolName, cephId: ceph.id))
            } else if (!simulator.sqlite.existById(cmd.poolName, CephPrimaryStoragePool.class)) {
                rsp.setError("no pool[name:${cmd.poolName}] found")
                return rsp
            }

            List<CephPoolCapacity> poolCapacities = []
            long capacity = 819200

            rsp.totalCapacity = ceph.totalCapacity + capacity
            rsp.availableCapacity = ceph.totalCapacity + capacity
            poolCapacities.add(new CephPoolCapacity(
                    name: cmd.poolName,
                    availableCapacity: capacity,
                    usedCapacity: 0,
                    totalCapacity: capacity,
                    relatedOsds: "osd.1"
            ))
            rsp.poolCapacities = poolCapacities
            rsp.type = CephConstants.CEPH_MANUFACTURER_OPENSOURCE
            return rsp
        }

        handle(CephPrimaryStorageBase.CHECK_BITS_PATH) {
            CephPrimaryStorageBase.CheckIsBitsExistingRsp rsp = new CephPrimaryStorageBase.CheckIsBitsExistingRsp()
            rsp.setExisting(true)
            return rsp
        }

        handle(CephPrimaryStorageMonBase.PING_PATH) {
            CephPrimaryStorageMonBase.PingRsp rsp = new CephPrimaryStorageMonBase.PingRsp()
            rsp.success = true
            return rsp
        }

        handle(CephPrimaryStorageBase.CEPH_TO_CEPH_MIGRATE_VOLUME_SEGMENT_PATH) {
            return new CephPrimaryStorageBase.StorageMigrationRsp()
        }

        handle(CephPrimaryStorageBase.GET_VOLUME_SNAPINFOS_PATH) {
            return new CephPrimaryStorageBase.GetVolumeSnapInfosRsp()
        }

        handle(CephPrimaryStorageBase.DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
            CephPrimaryStorageBase.DownloadBitsFromKVMHostRsp rsp = new CephPrimaryStorageBase.DownloadBitsFromKVMHostRsp()
            rsp.format = "raw"
            return new CephPrimaryStorageBase.AgentResponse()
        }

        handle(CephPrimaryStorageBase.CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
            return new CephPrimaryStorageBase.AgentResponse()
        }

        handle(CephPrimaryStorageBase.GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH) {
            CephPrimaryStorageBase.GetDownloadBitsFromKVMHostProgressRsp rsp = new CephPrimaryStorageBase.GetDownloadBitsFromKVMHostProgressRsp()
            rsp.totalSize = 1L
            return rsp
        }

        handle(CephPrimaryStorageBase.GET_IMAGE_WATCHERS_PATH) {
            return new CephPrimaryStorageBase.GetVolumeWatchersRsp()
        }

        handle(CephPrimaryStorageBase.CLAEN_TRASH_PATH) {
            return new CephPrimaryStorageBase.CleanTrashRsp()
        }
    }
}
