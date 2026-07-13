package org.zstack.testlib

import org.apache.commons.lang.StringEscapeUtils
import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.ceph.CephConstants
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.CephSystemTags
import org.zstack.storage.ceph.DataSecurityPolicy
import org.zstack.storage.ceph.primary.*
import org.zstack.testlib.vfs.CephRaw
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vfs.VFSFile
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.nio.file.Path

/**
 * Created by xing5 on 2017/2/20.
 */
class CephPrimaryStorageSpec extends PrimaryStorageSpec {
    @SpecParam(required = true)
    String fsid
    @SpecParam(required = true)
    List<String> monUrls
    @SpecParam
    Map<String, String> monAddrs = [:]
    @SpecParam
    String rootVolumePoolName = "pri-v-r-" + Platform.getUuid()
    @SpecParam
    String dataVolumePoolName = "pri-v-d-" + Platform.getUuid()
    @SpecParam
    String imageCachePoolName = "pri-c-" + Platform.getUuid()

    CephPrimaryStorageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    static VFS vfs(CephPrimaryStorageBase.AgentCommand cmd, EnvSpec env, boolean errorOnNotExisting=false) {
        return vfs1(cmd.fsId, env, errorOnNotExisting)
    }

    static VFS vfs1(String fsId, EnvSpec env, boolean errorOnNotExisting=false) {
        return env.getVirtualFileSystem(fsId, errorOnNotExisting)
    }

    static void manuallyAddCephPool(String primaryStorageUuid, String poolName, EnvSpec env) {
        String fsId = Q.New(CephPrimaryStorageVO.class)
                .select(CephPrimaryStorageVO_.fsid)
                .eq(CephPrimaryStorageVO_.uuid, primaryStorageUuid)
                .findValue()
        VFS vfs = env.getVirtualFileSystem(fsId, true)
        vfs.createDirectories(cephPathToVFSPath(poolName))
    }

    static String cephPathToVFSPath(String str) {
        str = str.replaceAll("ceph://", "")
        return str.startsWith("/") ? str : "/" + str
    }


    static List<VFSFile> getSnapshotPaths(VFS VFS, String volumePath) {
        List<VFSFile> ret = new ArrayList<>()
        VFS.walkFileSystem({ vfile ->
            if (vfile.pathString().contains(volumePath) && vfile.pathString() != volumePath) {
                ret.add(vfile)
            }
        })
        return ret
    }

    class CephPrimaryStorageStruct {
        String rootVolumePoolName
        String dataVolumePoolName
        String imageCachePoolName
    }

    static class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec espec) {
            def simulator = { arg1, arg2 ->
                espec.simulator(arg1, arg2)
            }

            simulator(CephPrimaryStorageBase.GET_FACTS) { HttpEntity<String> e, EnvSpec spec ->
                CephPrimaryStorageBase.GetFactsCmd cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.GetFactsCmd.class)
                CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)
                assert cspec != null: "cannot find ceph primary storage[uuid:${cmd.uuid}], check your environment()"

                def rsp = new CephPrimaryStorageBase.GetFactsRsp()
                rsp.fsid = cspec.fsid

                String monAddr = Q.New(CephPrimaryStorageMonVO.class)
                        .select(CephPrimaryStorageMonVO_.monAddr).eq(CephPrimaryStorageMonVO_.uuid, cmd.monUuid).findValue()

                rsp.monAddr = cspec.monAddrs[(monAddr)]

                return rsp
            }

            simulator(CephPrimaryStorageBase.DELETE_POOL_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.DeletePoolRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.DELETE_POOL_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                CephPrimaryStorageBase.DeletePoolCmd cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeletePoolCmd.classes)
                VFS vfs = vfs(cmd, spec, true)
                cmd.poolNames.each { vfs.delete(cephPathToVFSPath(it)) }
                return rsp
            }

            simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)
                CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)
                assert cspec != null: "cannot find ceph primary storage[uuid:${cmd.uuid}], check your environment()"

                def rsp = new CephPrimaryStorageBase.InitRsp()
                rsp.fsid = cspec.fsid
                rsp.userKey = Platform.uuid
                rsp.totalCapacity = cspec.totalCapacity
                rsp.availableCapacity = cspec.availableCapacity
                long rootSize = cspec.availableCapacity / 3
                long dataSize = cspec.availableCapacity / 3
                long cacheSize = cspec.totalCapacity - rootSize - dataSize
                List<CephPoolCapacity> poolCapacities = [
                        new CephPoolCapacity(
                                name: cspec.rootVolumePoolName,
                                availableCapacity: rootSize,
                                usedCapacity: cspec.totalCapacity - cspec.availableCapacity,
                                totalCapacity: rootSize,
                                securityPolicy: DataSecurityPolicy.Copy.toString(),
                                replicatedSize: 3,
                                diskUtilization: 0.33,
                                relatedOsds: 'osd.1'
                        ),
                        new CephPoolCapacity(
                                name: cspec.dataVolumePoolName,
                                availableCapacity: dataSize,
                                usedCapacity: 0,
                                totalCapacity: dataSize,
                                securityPolicy: DataSecurityPolicy.Copy.toString(),
                                replicatedSize: 3,
                                diskUtilization: 0.33,
                                relatedOsds: 'osd.2'
                        ),
                        new CephPoolCapacity(
                                name: cspec.imageCachePoolName,
                                availableCapacity: cacheSize,
                                usedCapacity: 0,
                                totalCapacity: cacheSize,
                                securityPolicy: DataSecurityPolicy.Copy.toString(),
                                replicatedSize: 3,
                                diskUtilization: 0.33,
                                relatedOsds: 'osd.3'
                        ),
                ]
                rsp.poolCapacities = poolCapacities
                rsp.type = CephConstants.CEPH_MANUFACTURER_OPENSOURCE
                return rsp
            }

            VFS.vfsHook(CephPrimaryStorageBase.INIT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)

                CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)

                if (cspec == null) {
                    cmd.fsId = rsp.fsid
                } else {
                    cmd.fsId = cspec.fsid
                }

                assert cmd.fsId != null : "cannot find ceph primary storage [uuid:${cmd.uuid}] fsid, check your environment() and simulator"

                VFS vfs = vfs(cmd, spec)

                // if spec has those pool names defined, it means those pools are pre-created
                // in ceph, here we simulate the pre-created logic
                if (cspec != null) {
                    if (cspec.rootVolumePoolName != null) {
                        String dir = cephPathToVFSPath(cspec.rootVolumePoolName)
                        if (!vfs.exists(dir)) {
                            vfs.createDirectories(dir)
                        }
                    }

                    if (cspec.dataVolumePoolName != null) {
                        String dir = cephPathToVFSPath(cspec.dataVolumePoolName)
                        if (!vfs.exists(dir)) {
                            vfs.createDirectories(dir)
                        }
                    }

                    if (cspec.imageCachePoolName != null) {
                        String dir = cephPathToVFSPath(cspec.imageCachePoolName)
                        if (!vfs.exists(dir)) {
                            vfs.createDirectories(dir)
                        }
                    }
                } else {
                    List<CephPrimaryStoragePoolVO> pools = Q.New(CephPrimaryStoragePoolVO.class).eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, cmd.uuid).list()

                    for (CephPrimaryStoragePoolVO pool : pools) {
                        if (pool.type == CephPrimaryStoragePoolType.Root.toString() && CephSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.hasTag(cmd.uuid)
                        || pool.type == CephPrimaryStoragePoolType.Data.toString() && CephSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.hasTag(cmd.uuid)
                        || pool.type == CephPrimaryStoragePoolType.ImageCache.toString() && CephSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.hasTag(cmd.uuid)) {
                            String dir = cephPathToVFSPath(pool.poolName)
                            if (!vfs.exists(dir)) {
                                vfs.createDirectories(dir)
                            }
                        }
                    }
                }

                cmd.pools.each { CephPrimaryStorageBase.Pool pool ->
                    String dir = cephPathToVFSPath(pool.name)
                    if (pool.predefined) {
                        vfs.Assert(vfs.isDir(dir), "cannot find ceph pool[${pool.name}]")
                    } else {
                        vfs.createDirectories(dir)
                    }
                }

                return rsp
            }

            simulator(CephPrimaryStorageBase.CHECK_POOL_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.CheckRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.CHECK_POOL_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CheckCmd.class)
                CephPrimaryStorageSpec bspec = spec.specByUuid(cmd.uuid)
                assert bspec != null: "cannot find the primary storage[uuid:${cmd.uuid}}, check your environment()"

                VFS vfs = vfs(cmd, spec)
                cmd.pools.each {
                    vfs.Assert(vfs.isDir(cephPathToVFSPath(it.name)), "cannot find ceph pool[${it.name}]")
                }

                return rsp
            }

            simulator(CephPrimaryStorageBase.CREATE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.CreateEmptyVolumeRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.CREATE_VOLUME_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CreateEmptyVolumeCmd.class)
                VFS vfs = vfs(cmd, spec)
                String path = cephPathToVFSPath(cmd.installPath)
                if (!(cmd.skipIfExisting && vfs.exists(path))) {
                    vfs.createCephRaw(path, cmd.size)
                }

                return rsp
            }

            simulator(CephPrimaryStorageBase.LUKS_SWAP_IN_PLACE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.AgentResponse()
            }

            VFS.vfsHook(CephPrimaryStorageBase.LUKS_SWAP_IN_PLACE_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.SwapInPlaceLuksRbdCmd.class)
                VFS vfs = vfs(cmd, spec)
                String srcPath = cephPathToVFSPath(cmd.installPath)
                String tmpPath = cephPathToVFSPath(cmd.temporaryInstallPath)
                String oldPath = "${srcPath}-plain-${Platform.uuid.substring(0, 8)}"

                [srcPath, tmpPath].each {
                    vfs.Assert(!it.contains("@"), "RBD LUKS in-place swap only supports active image paths[${it}]")
                }
                vfs.Assert(vfs.isFile(srcPath), "cannot find the source file[${cmd.installPath}]")
                CephRaw src = vfs.getFile(srcPath, true)
                if (!vfs.exists(tmpPath)) {
                    CephRaw tmp = vfs.createCephRaw(tmpPath, src.virtualSize)
                    tmp.actualSize = src.actualSize
                    tmp.update()
                }
                vfs.Assert(vfs.isFile(tmpPath), "cannot find the temporary file[${cmd.temporaryInstallPath}]")
                vfs.Assert(!vfs.exists(oldPath), "old file[${oldPath}] already exists")

                src.move(oldPath)
                CephRaw tmp = vfs.getFile(tmpPath, true)
                tmp.move(srcPath)
                vfs.delete(oldPath)

                return rsp
            }

            simulator(CephPrimaryStorageBase.KVM_CREATE_SECRET_PATH) {
                return new KVMAgentCommands.AgentResponse()
            }

            simulator(CephPrimaryStorageBase.CHECK_HOST_STORAGE_CONNECTION_PATH) { HttpEntity<String> e ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CheckHostStorageConnectionCmd)
                assert cmd.hostUuid != null
                return new KVMAgentCommands.AgentResponse()
            }

            simulator(CephPrimaryStorageBase.DELETE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.DeleteRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.DELETE_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteCmd.class)
                VFS vfs = vfs(cmd, spec)

                if (!vfs.exists(cephPathToVFSPath(cmd.installPath))) {
                    return rsp
                }

                vfs.delete(cephPathToVFSPath(cmd.installPath))

                return rsp
            }

            simulator(CephPrimaryStorageMonBase.ECHO_PATH) { HttpEntity<String> entity ->
                Spec.checkHttpCallType(entity, true)
                return [:]
            }

            simulator(CephPrimaryStorageBase.CREATE_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CreateSnapshotCmd)
                assert !cmd.snapshotPath.contains("null")
                def rsp = new CephPrimaryStorageBase.CreateSnapshotRsp()
                rsp.size = 0
                rsp.installPath = cmd.snapshotPath
                return rsp
            }

            VFS.vfsHook(CephPrimaryStorageBase.CREATE_SNAPSHOT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CreateSnapshotCmd.class)

                VFS vfs = vfs(cmd, spec)
                String snapPath = cephPathToVFSPath(cmd.snapshotPath)
                String volumePath  = snapPath.split("@")[0]
                vfs.Assert(vfs.exists(volumePath), "cannot find file[${volumePath}]")

                if (!(cmd.skipOnExisting && vfs.exists(snapPath))) {
                    vfs.createCephRaw(snapPath, 0L)
                }

                return rsp
            }

            simulator(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.DeleteSnapshotRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteSnapshotCmd.class)
                VFS vfs = vfs(cmd, spec)
                String snapPath = cephPathToVFSPath(cmd.snapshotPath)
                vfs.delete(snapPath)

                return rsp
            }

            simulator(CephPrimaryStorageBase.PURGE_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.PurgeSnapshotRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.PURGE_SNAPSHOT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.PurgeSnapshotCmd.class)
                VFS vfs = vfs(cmd, spec)

                List<VFSFile> toDelete = []

                String volPath = cephPathToVFSPath(cmd.volumePath)
                vfs.walkFileSystem { f ->
                    if (f.pathString().contains(volPath) && f.pathString().contains("@")) {
                        toDelete.add(f)
                    }
                }

                toDelete.each { it.delete() }
                return rsp
            }

            simulator(CephPrimaryStorageBase.PROTECT_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.ProtectSnapshotRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.PROTECT_SNAPSHOT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.ProtectSnapshotCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.Assert(vfs.exists(cephPathToVFSPath(cmd.snapshotPath)), "cannot find the snapshot[${cmd.snapshotPath}]")
                return rsp
            }

            simulator(CephPrimaryStorageBase.UNPROTECT_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.UnprotectedSnapshotRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.UNPROTECT_SNAPSHOT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.UnprotectedSnapshotCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.Assert(vfs.exists(cephPathToVFSPath(cmd.snapshotPath)), "cannot find the snapshot[${cmd.snapshotPath}]")
                return new CephPrimaryStorageBase.UnprotectedSnapshotRsp()
            }

            simulator(CephPrimaryStorageBase.CLONE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CloneCmd.class)
                def rsp = new CephPrimaryStorageBase.CloneRsp()
                rsp.size = 0
                rsp.actualSize = 0
                rsp.installPath = cmd.dstPath
                return rsp
            }

            VFS.vfsHook(CephPrimaryStorageBase.CLONE_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CloneCmd.class)
                VFS vfs = vfs(cmd, spec)
                String srcPath = cephPathToVFSPath(cmd.srcPath)
                vfs.Assert(vfs.isFile(srcPath), "cannot find the source file[${srcPath}]")
                vfs.createCephRaw(cephPathToVFSPath(cmd.dstPath), 0L, srcPath)

                return rsp
            }

            simulator(CephPrimaryStorageBase.FLATTEN_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.FlattenRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.FLATTEN_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.FlattenCmd.class)
                VFS vfs = vfs(cmd, spec)
                CephRaw f = vfs.getFile(cephPathToVFSPath(cmd.path), true)
                f.flatten()

                return rsp
            }

            simulator(CephPrimaryStorageBase.CP_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CpCmd.class)
                def rsp = new CephPrimaryStorageBase.CpRsp()
                rsp.size = 0
                rsp.installPath = cmd.dstPath
                return rsp
            }

            simulator(CephPrimaryStorageBase.GET_VOLUME_SIZE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.GetVolumeSizeCmd.class)
                CephPrimaryStorageBase.GetVolumeSizeRsp rsp = new CephPrimaryStorageBase.GetVolumeSizeRsp()
                Long size = Q.New(VolumeVO.class).select(VolumeVO_.size).eq(VolumeVO_.uuid, cmd.volumeUuid).findValue()
                rsp.actualSize = null
                rsp.size = size
                return rsp
            }

            simulator(CephPrimaryStorageBase.GET_VOLUME_SNAPSHOT_SIZE_PATH) {
                def rsp = new CephPrimaryStorageBase.GetVolumeSnapshotSizeRsp()
                return rsp
            }

            simulator(CephPrimaryStorageBase.BATCH_GET_VOLUME_SIZE_PATH) {
                return new CephPrimaryStorageBase.GetBatchVolumeSizeRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.CP_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CpCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.Assert(vfs.exists(cephPathToVFSPath(cmd.srcPath)), "cannot find the source file[${cmd.srcPath}]")
                vfs.createCephRaw(cephPathToVFSPath(cmd.dstPath), 0L)

                return rsp
            }

            VFS.vfsHook(CephPrimaryStorageBase.GET_VOLUME_SIZE_PATH, espec) { CephPrimaryStorageBase.GetVolumeSizeRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.GetVolumeSizeCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.Assert(vfs.isFile(cephPathToVFSPath(cmd.installPath)), "cannot find the volume[${cmd.installPath}]")
                CephRaw f = vfs.getFile(cephPathToVFSPath(cmd.installPath))
                rsp.size = f.getVirtualSize()
                if (rsp.type == CephConstants.CEPH_MANUFACTURER_OPENSOURCE) {
                    rsp.actualSize = null
                } else {
                    rsp.actualSize = f.getActualSize()
                }
                return rsp
            }

            simulator(CephPrimaryStorageBase.GET_IMAGE_WATCHERS_PATH) {
                def rsp = new CephPrimaryStorageBase.GetVolumeWatchersRsp()
                return rsp
            }

            simulator(CephPrimaryStorageBase.ROLLBACK_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.RollbackSnapshotRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.ROLLBACK_SNAPSHOT_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.RollbackSnapshotCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.Assert(vfs.isFile(cephPathToVFSPath(cmd.snapshotPath)), "cannot find the snapshot[${cmd.snapshotPath}]")
                String[] p = cmd.snapshotPath.split("@")
                String volumePath = p[0]
                vfs.Assert(vfs.isFile(cephPathToVFSPath(volumePath)), "cannot find the volume[${volumePath}] of the snapshot[${cmd.snapshotPath}]")

                return rsp
            }

            simulator(CephPrimaryStorageBase.KVM_HA_SETUP_SELF_FENCER) {
                return new CephPrimaryStorageBase.AgentResponse()
            }

            simulator(CephPrimaryStorageBase.KVM_HA_CANCEL_SELF_FENCER) {
                return new CephPrimaryStorageBase.AgentResponse()
            }

            simulator(CephPrimaryStorageBase.DELETE_IMAGE_CACHE) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.AgentResponse()
            }

            simulator(CephPrimaryStorageBase.ADD_POOL_PATH) { HttpEntity<String> entity, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(entity.body, CephPrimaryStorageBase.AddPoolCmd.class)

                CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)
                CephPrimaryStorageBase.AddPoolRsp rsp = new CephPrimaryStorageBase.AddPoolRsp()
                rsp.totalCapacity = cspec.totalCapacity
                rsp.availableCapacity = cspec.availableCapacity
                long rootSize = cspec.availableCapacity / 3
                long dataSize = cspec.availableCapacity / 3
                long cacheSize = cspec.totalCapacity - rootSize - dataSize
                rsp.setAvailableCapacity(SizeUnit.GIGABYTE.toByte(100))
                rsp.setTotalCapacity(SizeUnit.GIGABYTE.toByte(100))
                List<CephPoolCapacity> poolCapacities = [
                        new CephPoolCapacity(
                                name: cspec.rootVolumePoolName,
                                availableCapacity: rootSize,
                                usedCapacity: cspec.totalCapacity - cspec.availableCapacity,
                                totalCapacity: rootSize,
                                securityPolicy: DataSecurityPolicy.Copy.toString(),
                                replicatedSize: 3,
                                diskUtilization: 0.33,
                                relatedOsds: 'osd.1'
                        ),
                        new CephPoolCapacity(
                                name: cspec.dataVolumePoolName,
                                availableCapacity: dataSize,
                                usedCapacity: 0,
                                totalCapacity: dataSize,
                                securityPolicy: DataSecurityPolicy.Copy.toString(),
                                replicatedSize: 3,
                                diskUtilization: 0.33,
                                relatedOsds: 'osd.2'
                        ),
                        new CephPoolCapacity(
                                name: cspec.imageCachePoolName,
                                availableCapacity: cacheSize,
                                usedCapacity: 0,
                                totalCapacity: cacheSize,
                                securityPolicy: DataSecurityPolicy.Copy.toString(),
                                replicatedSize: 3,
                                diskUtilization: 0.33,
                                relatedOsds: 'osd.3'
                        ),
                        new CephPoolCapacity(
                                name: cmd.poolName,
                                availableCapacity: SizeUnit.GIGABYTE.toByte(100),
                                usedCapacity: 0,
                                totalCapacity: SizeUnit.GIGABYTE.toByte(100),
                                securityPolicy: DataSecurityPolicy.Copy.toString(),
                                replicatedSize: 3,
                                diskUtilization: 0.33,
                                relatedOsds: "osd.4"
                        )
                ]
                rsp.setPoolCapacities(poolCapacities)

                return rsp
            }

            VFS.vfsHook(CephPrimaryStorageBase.DELETE_IMAGE_CACHE, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteImageCacheCmd.class)
                VFS vfs = vfs(cmd, spec)
                String imagePath = cephPathToVFSPath(cmd.imagePath)
                String snapshotPath = cephPathToVFSPath(cmd.snapshotPath)
                vfs.Assert(vfs.isFile(imagePath), "cannot find the image[${imagePath}]")
                vfs.Assert(vfs.isFile(snapshotPath), "cannot find the snapshot[${snapshotPath}]")
                List<String> children = []
                vfs.walkFileSystem { f ->
                    if (!(f instanceof CephRaw)) {
                        return
                    }

                    if (f.parent?.setVolumeChainInstallPaths() == snapshotPath) {
                        children.add(f.pathString())
                    }
                }

                vfs.Assert(children.isEmpty(), "the image[${imagePath}, snapshot: ${snapshotPath}] still has children: ${children}")
                CephRaw f = vfs.getFile(snapshotPath, true)
                f.delete()
                f = vfs.getFile(imagePath, true)
                f.delete()

                return rsp
            }

            VFS.vfsHook(CephPrimaryStorageBase.ADD_POOL_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.AddPoolCmd.class)
                VFS vfs = vfs(cmd, spec)

                // the poolName is unicode string escaped like \u0068\u0069\u0067\u0068\u005f\u0070\u006f\u006f\u006c
                String poolName = StringEscapeUtils.unescapeJava(cmd.poolName)
                String poolPath = cephPathToVFSPath(poolName)
                if (cmd.isCreate) {
                    vfs.Assert(!vfs.exists(poolPath), "${poolName} already exists")
                } else {
                    vfs.Assert(vfs.exists(poolPath), "${poolName} not existing, isCreate == false which means you need to manually create it")
                }
                vfs.createDirectories(poolPath)

                return rsp
            }

            simulator(CephPrimaryStorageBase.CHECK_BITS_PATH) { HttpEntity<String> e, EnvSpec spec ->
                CephPrimaryStorageBase.CheckIsBitsExistingRsp rsp = new CephPrimaryStorageBase.CheckIsBitsExistingRsp()
                rsp.setExisting(true)
                return rsp
            }

            VFS.vfsHook(CephPrimaryStorageBase.CHECK_BITS_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CheckIsBitsExistingCmd.class)
                VFS vfs = vfs(cmd, spec)
                if (cmd.installPath ==~ /.*[0-9a-f]{32}\.(encrypted|plain)\.[0-9a-f]{32}$/) {
                    rsp.existing = vfs.exists(cephPathToVFSPath(cmd.installPath))
                } else {
                    vfs.Assert(vfs.isFile(cephPathToVFSPath(cmd.installPath)), "cannot find ${cmd.installPath}")
                }

                return rsp
            }

            simulator(CephPrimaryStorageMonBase.PING_PATH) {
                CephPrimaryStorageMonBase.PingRsp rsp = new CephPrimaryStorageMonBase.PingRsp()
                rsp.success = true
                return rsp
            }

            simulator(CephPrimaryStorageBase.DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                CephPrimaryStorageBase.DownloadBitsFromKVMHostRsp rsp = new CephPrimaryStorageBase.DownloadBitsFromKVMHostRsp()
                rsp.format = 'raw'
                return rsp
            }

            simulator(CephPrimaryStorageBase.DOWNLOAD_BITS_FROM_KVM_HOST_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.AgentResponse()
            }

            VFS.vfsHook(CephPrimaryStorageBase.DOWNLOAD_BITS_FROM_KVM_HOST_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DownloadBitsFromKVMHostCmd.class)
                VFS vfs = vfs(cmd, spec)
                String installPath = cephPathToVFSPath(cmd.primaryStorageInstallPath)
                Path poolPath = vfs.getPath(installPath).getRoot()
                vfs.Assert(vfs.isDir(poolPath), "cannot find the pool[${poolPath}], ${e.body}")

                if (!vfs.exists(installPath)) {
                    vfs.createCephRaw(installPath, 0L)
                }

                CephPrimaryStorageBase.DownloadBitsFromKVMHostRsp response = new CephPrimaryStorageBase.DownloadBitsFromKVMHostRsp()
                response.format = 'raw'

                return response
            }

            simulator(CephPrimaryStorageBase.CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                return new CephPrimaryStorageBase.AgentResponse()
            }

            simulator(CephPrimaryStorageBase.CEPH_TO_CEPH_MIGRATE_VOLUME_SEGMENT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.StorageMigrationRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.CEPH_TO_CEPH_MIGRATE_VOLUME_SEGMENT_PATH, espec) { rsp,  HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CephToCephMigrateVolumeSegmentCmd.class)
                VFS vfs = vfs(cmd, spec)

                String srcInstallPath = cephPathToVFSPath(cmd.srcInstallPath)
                vfs.Assert(vfs.isFile(srcInstallPath), "cannot find the source file[${cmd.srcInstallPath}]")

                CephRaw srcRaw = vfs.getFile(srcInstallPath)

                String dstPrimaryStorageUuid = Q.New(CephPrimaryStorageMonVO.class)
                        .select(CephPrimaryStorageMonVO_.primaryStorageUuid)
                        .eq(CephPrimaryStorageMonVO_.hostname, cmd.dstMonHostname)
                        .findValue()
                String dstFsid = Q.New(CephPrimaryStorageVO.class)
                        .select(CephPrimaryStorageVO_.fsid)
                        .eq(CephPrimaryStorageVO_.uuid, dstPrimaryStorageUuid)
                        .findValue()
                VFS dstVfs = vfs1(dstFsid, spec, true)
                // confirm dst volume created
                String dstInstallPath = cephPathToVFSPath(cmd.dstInstallPath)
                vfs.Assert(dstVfs.exists(dstInstallPath), "dst volume not found")

                if (srcInstallPath.contains("@")) {
                    dstInstallPath = String.format("%s@%s", dstInstallPath, cmd.resourceUuid)
                } else {
                    dstVfs.delete(dstInstallPath)
                }

                // create new one with right virtual size
                dstVfs.createCephRaw(dstInstallPath, srcRaw.virtualSize)

                return rsp
            }

            simulator(CephPrimaryStorageBase.GET_VOLUME_SNAPINFOS_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.GetVolumeSnapInfosRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.GET_VOLUME_SNAPINFOS_PATH, espec) { CephPrimaryStorageBase.GetVolumeSnapInfosRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.GetVolumeSnapInfosCmd.class)
                VFS vfs = vfs(cmd, spec)
                String vfsPath = cephPathToVFSPath(cmd.volumePath)
                vfs.Assert(vfs.exists(vfsPath), "cannot find the volume[${cmd.volumePath}]")
                List<VFSFile> files = getSnapshotPaths(vfs, vfsPath)

                rsp.setSnapInfos(new ArrayList<CephPrimaryStorageBase.SnapInfo>())
                files.each { file ->
                    if (!(file instanceof CephRaw)) {
                        return
                    }
                    CephRaw cephRaw = (CephRaw) file

                    CephPrimaryStorageBase.SnapInfo snapInfo = new CephPrimaryStorageBase.SnapInfo()
                    snapInfo.size = cephRaw.virtualSize
                    snapInfo.id = 1
                    snapInfo.name = cephRaw.pathString().split("@")[1]
                    rsp.getSnapInfos().add(snapInfo)
                }

                return rsp
            }

            simulator(CephPrimaryStorageBase.GET_BACKING_CHAIN_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.GetBackingChainRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.GET_BACKING_CHAIN_PATH, espec) { CephPrimaryStorageBase.GetBackingChainRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.GetBackingChainCmd.class)
                VFS vfs = vfs(cmd, spec)
                String vfsPath = cephPathToVFSPath(cmd.volumePath)
                vfs.Assert(vfs.exists(vfsPath), "cannot find the volume[${cmd.volumePath}]")

                CephRaw file = vfs.getFile(vfsPath)

                while (file.parent != null) {
                    rsp.backingChain.add("ceph:/" + file.pathString())
                    String parentPath = file.parent.pathString().split("@")[0]
                    vfs.Assert(vfs.exists(parentPath), "cannot find the parent[${file}]")
                    file = vfs.getFile(parentPath)
                }

                return rsp
            }

            simulator(CephPrimaryStorageBase.DELETE_VOLUME_CHAIN_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteVolumeChainCmd.class)
                def rsp = new CephPrimaryStorageBase.DeleteVolumeChainRsp()
                rsp.undeletedInstallPaths = []
                return rsp
            }

            // TODO Need to implement ceph trash in VFS
            VFS.vfsHook(CephPrimaryStorageBase.DELETE_VOLUME_CHAIN_PATH, espec) { CephPrimaryStorageBase.DeleteVolumeChainRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteVolumeChainCmd.class)
                VFS vfs = vfs(cmd, spec)
                rsp.undeletedInstallPaths = []

                for (String path : cmd.installPaths) {
                    String vfsPath = cephPathToVFSPath(path)
                    vfs.Assert(vfs.exists(vfsPath), "cannot find the volume[${vfsPath}]")

                    if (vfsPath.contains("@")) {
                        vfs.delete(vfsPath)
                        String volPath = vfsPath.split("@")[0]
                        assert getSnapshotPaths(vfs, volPath).isEmpty() : "the volume[%s] has snapshots, cannot delete it".format(volPath)
                        vfs.delete(volPath)
                    } else {
                        assert getSnapshotPaths(vfs, vfsPath).isEmpty() : "the volume[%s] has snapshots, cannot delete it".format(vfsPath)
                        vfs.delete(vfsPath)
                    }
                }
                return rsp
            }

            simulator(CephPrimaryStorageBase.CLAEN_TRASH_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.CleanTrashRsp()
            }

            simulator(CephPrimaryStorageBase.DOWNLOAD_BITS_FROM_REMOTE_TARGET_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephPrimaryStorageBase.DownloadBitsFromRemoteTargetRsp()
            }

            VFS.vfsHook(CephPrimaryStorageBase.DOWNLOAD_BITS_FROM_REMOTE_TARGET_PATH, espec) { CephPrimaryStorageBase.AgentResponse rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DownloadBitsFromRemoteTargetCmd.class)
                VFS vfs = vfs(cmd, spec)

                def pool = cephPathToVFSPath(cmd.primaryStorageInstallPath).split("/")[1]
                def dir = cephPathToVFSPath(pool)
                if (!vfs.exists(dir)) {
                    vfs.createDirectories(dir)
                }

                vfs.createCephRaw(cephPathToVFSPath(cmd.primaryStorageInstallPath), 0L)
                return rsp
            }
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addCephPrimaryStorage {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.url = url
            delegate.sessionId = sessionId
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.monUrls = monUrls
            delegate.rootVolumePoolName = rootVolumePoolName
            delegate.dataVolumePoolName = dataVolumePoolName
            delegate.imageCachePoolName = imageCachePoolName
        } as PrimaryStorageInventory

        postCreate {
            inventory = queryCephPrimaryStorage {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    CephPrimaryStoragePoolSpec pool(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = CephPrimaryStoragePoolSpec.class) Closure c) {
        def spec = new CephPrimaryStoragePoolSpec(envSpec)
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = spec
        c()
        addChild(spec)
        return spec
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deletePrimaryStorage {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
