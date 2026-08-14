package org.zstack.zsv;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageBootMode;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.database.backup.DatabaseBackupVersionExtensionPoint;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.storageDevice.ScsiLunVmInstanceRefVO;
import org.zstack.header.storageDevice.ScsiLunVmInstanceRefVO_;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeDeletionExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.image.ImageDefaultBehavior;
import org.zstack.mevoco.DeployMode;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO_;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig;
import org.zstack.storage.snapshot.group.RevertVmFromSnapShotGroupExtension;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupOperationValidator;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.string.StringSimilarity;
import org.zstack.zsv.core.api.APIGetNodeRolesMsg;
import org.zstack.zsv.core.api.APIGetNodeRolesReply;
import org.zstack.zsv.core.entity.NodeRolesItemView;
import org.zstack.zsv.core.entity.NodeRolesView;
import org.zstack.zsv.core.header.GetManagementNodeRolesMsg;
import org.zstack.zsv.core.header.GetManagementNodeRolesReply;
import org.zstack.zsv.snapshotgroup.ZsvBeforeRevertSnapshotGroupFlow;

import javax.persistence.Tuple;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.zsv.ZsvConstant.*;

/**
 * @ Author : yh.w
 * @ Date   : Created in 14:45 2023/7/31
 */
public class ZsvManagerImpl extends AbstractService implements ZsvManager, ManagementNodeReadyExtensionPoint,
        VmAttachVolumeExtensionPoint, VolumeDeletionExtensionPoint, RevertVmFromSnapShotGroupExtension,
        DatabaseBackupVersionExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ZsvManagerImpl.class);

    private static final List<VmInstanceState> ignoredSnapshotGroupCheckVmStates = Arrays.asList(VmInstanceState.Destroyed,
            VmInstanceState.Destroying,
            VmInstanceState.Expunging);
    private static final Map<String, String> zsvVersionMap = new HashMap<>();

    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        ImageDefaultBehavior.setDefaultPlatform(null);
        ImageDefaultBehavior.setDefaultBootMode(null);

        StringSimilarity.elaborateFolder = "zsvErrorElaborations";
        StringSimilarity.refreshErrorTemplates();
        return true;
    }

    @Override
    public boolean stop() {
        ImageDefaultBehavior.setDefaultPlatform(ImagePlatform.Linux.toString());
        ImageDefaultBehavior.setDefaultBootMode(ImageBootMode.Legacy.toString());
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof GetManagementNodeRolesMsg) {
            handle((GetManagementNodeRolesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetNodeRolesMsg) {
            handle((APIGetNodeRolesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetNodeRolesMsg msg) {
        List<ManagementNodeVO> managementNodes = Q.New(ManagementNodeVO.class)
                .list();

        APIGetNodeRolesReply reply = new APIGetNodeRolesReply();
        reply.setInventories(new ArrayList<>());

        new While<>(managementNodes).step((node, whileCompletion) -> {
            GetManagementNodeRolesMsg innerMsg = new GetManagementNodeRolesMsg();
            bus.makeServiceIdByManagementNodeId(innerMsg, ZsvConstant.SERVICE_ID, node.getUuid());
            bus.send(innerMsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply messageReply) {
                    if (!messageReply.isSuccess()) {
                        logger.warn(String.format("failed to check if management node is also compute node, node: %s", node.getUuid()));
                        whileCompletion.done();
                        return;
                    }

                    GetManagementNodeRolesReply innerReply = messageReply.castReply();
                    NodeRolesView view = new NodeRolesView();
                    view.setUuid(innerReply.getManagementNodeUuid());
                    view.setResourceType(ManagementNodeVO.class.getSimpleName());

                    List<NodeRolesItemView> items = new ArrayList<>();
                    view.setRoles(items);

                    NodeRolesItemView itemMN = new NodeRolesItemView();
                    itemMN.setUuid(innerReply.getManagementNodeUuid());
                    itemMN.setResourceType(ManagementNodeVO.class.getSimpleName());
                    itemMN.setRole(NODE_ROLE_MANAGEMENT);
                    items.add(itemMN);

                    if (innerReply.isHost()) {
                        NodeRolesItemView itemHost = new NodeRolesItemView();
                        itemHost.setResourceType(HostVO.class.getSimpleName());
                        itemHost.setRole(NODE_ROLE_COMPUTE);
                        if (innerReply.getHostUuid() != null) {
                            itemHost.setUuid(innerReply.getHostUuid());
                        } else {
                            logger.warn(String.format("management node %s is a host but no matching host found in database",
                                    innerReply.getManagementNodeUuid()));
                        }
                        items.add(itemHost);
                    }

                    synchronized (reply) {
                        reply.getInventories().add(view);
                    }
                    whileCompletion.done();
                }
            });
        }, 2).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetManagementNodeRolesMsg msg) {
        GetManagementNodeRolesReply reply = new GetManagementNodeRolesReply();
        reply.setManagementNodeUuid(Platform.getManagementServerId());

        File kvmAgentPidFile = new File(KVM_AGENT_PID_PATH);
        if (kvmAgentPidFile.exists()) {
            reply.setHost(true);

            String getALlIpCmd = "ip addr | awk -F '[/| ]+'  '/inet\\s+/{sub(/^\\s*/,\"\");print $2}'";
            ShellResult ret = ShellUtils.runAndReturn(getALlIpCmd);
            if (ret.getRetCode() != 0) {
                logger.warn(String.format("failed to get IPs from shell command, return code: %d, stderr: %s",
                        ret.getRetCode(), ret.getStderr()));
                bus.reply(msg, reply);
                return;
            }

            String stdout = ret.getStdout();
            if (stdout == null || stdout.trim().isEmpty()) {
                logger.warn("shell command returned empty output");
                bus.reply(msg, reply);
                return;
            }

            List<String> ips = Arrays.stream(stdout.split("\\n"))
                    .map(String::trim).filter(line -> !line.isEmpty()).collect(Collectors.toList());
            if (ips.isEmpty()) {
                bus.reply(msg, reply);
                return;
            }

            List<Tuple> tuples = Q.New(HostVO.class).select(HostVO_.uuid, HostVO_.managementIp).listValues();
            if (tuples == null || tuples.isEmpty()) {
                logger.warn("no hosts found in database");
                bus.reply(msg, reply);
                return;
            }

            for (Tuple tuple : tuples) {
                if (ips.contains(tuple.get(1, String.class))) {
                    reply.setHostUuid(tuple.get(0, String.class));
                    break;
                }
            }
        }
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public void managementNodeReady() {
        fillZsvVersionMap();
    }

    private boolean isSnapshotGroupExistVolume(String volUuid) {
        return Q.New(VolumeSnapshotGroupRefVO.class)
                .eq(VolumeSnapshotGroupRefVO_.volumeUuid, volUuid)
                .eq(VolumeSnapshotGroupRefVO_.snapshotDeleted, false)
                .isExists();
    }

    @Override
    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {
        if (isSnapshotGroupExistVolume(volume.getUuid()) &&
                !vm.getUuid().equals(volume.getLastVmInstanceUuid())) {
            throw new OperationFailureException(operr("volume %s still have snapshot group on vm %s, cannot attach to other vm",
                    volume.getUuid(), volume.getLastVmInstanceUuid()));
        }
    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data) {

    }

    @Override
    public void afterInstantiateVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data) {

    }

    @Override
    public void preDeleteVolume(VolumeInventory volume) {
        String vmUuid = volume.getVmInstanceUuid() == null ? volume.getLastVmInstanceUuid() : volume.getVmInstanceUuid();
        if (vmUuid == null) {
            return;
        }

        Tuple tuple = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.state)
                .eq(VmInstanceVO_.uuid, vmUuid).findTuple();
        if (tuple == null) {
            return;
        }

        VmInstanceState vmState = tuple.get(1, VmInstanceState.class);
        if (ignoredSnapshotGroupCheckVmStates.contains(vmState)) {
            return;
        }

        if (isSnapshotGroupExistVolume(volume.getUuid())) {
            throw new OperationFailureException(operr("volume %s still have snapshot group, cannot delete it", volume.getUuid()));
        }
    }

    @Override
    public void beforeDeleteVolume(VolumeInventory volume) {

    }

    @Override
    public void afterDeleteVolume(VolumeInventory volume, Completion completion) {
        completion.success();
    }

    @Override
    public void failedToDeleteVolume(VolumeInventory volume, ErrorCode errorCode) {

    }

    @Override
    public boolean needRunExtension() {
        return true;
    }

    @Override
    public Flow getBeforeRevertFlow() {
        return new ZsvBeforeRevertSnapshotGroupFlow();
    }

    @VolumeSnapshotGroupOperationValidator.VolumeSnapshotGroupCreationValidatorMethod
    @VolumeSnapshotGroupOperationValidator.VolumeSnapshotGroupReversionValidatorMethod
    public static void checkVmAttachedVolumesTypes(String vmUuid) {
        boolean sharableVolumeExist = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, vmUuid)
                .isExists();
        boolean lunExist = Q.New(ScsiLunVmInstanceRefVO.class)
                .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, vmUuid)
                .isExists();

        if (sharableVolumeExist || lunExist) {
            throw new OperationFailureException(operr("detach sharable volume or lun device before operating snapshot group"));
        }
    }

    // dbf.getDbVersion() retrieves the schema version, not the zsv version.
    // Refer to http://jira.zstack.io/browse/ZSV-4177 to correlate the zsv version with the schema version.
    // Read the files in the zsv/db directory to obtain the mapping relationship between the zsv version and the schema version.
    // For example: The content of file version 4.1.0 is as follows: ../upgrade/V4.8.0.1__schema.sql
    @AsyncThread
    private void fillZsvVersionMap() {
        String versionFolder = PathUtil.findFolderOnClassPath("db/zsv_ref/", true).getAbsolutePath();
        List<String> versionFiles = new ArrayList<>();
        PathUtil.scanFolder(versionFiles, versionFolder);
        versionFiles.forEach(versionFile -> {
            String content = PathUtil.readFileToString(versionFile, StandardCharsets.UTF_8);
            String schemaVersion = content.split("V")[1].split("__")[0];
            String version = versionFile.split("db/zsv_ref/")[1];
            zsvVersionMap.put(schemaVersion, version);
        });
    }

    @Override
    public String getVersion(String schemaVersion) {
        if (zsvVersionMap.get(schemaVersion) == null) {
            return schemaVersion;
        }
        return zsvVersionMap.get(schemaVersion);
    }

    @Override
    public String getDeployMode() {
        return DeployMode.zsv.toString();
    }
}
