package org.zstack.zmigrate.compute;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.accessKey.AccessKeyConstant;
import org.zstack.accessKey.AccessKeyVO;
import org.zstack.accessKey.AccessKeyVO_;
import org.zstack.accessKey.CreateAccessKeyMsg;
import org.zstack.accessKey.CreateAccessKeyReply;
import org.zstack.compute.vm.VmInstanceUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.AddImageMsg;
import org.zstack.header.image.AddImageReply;
import org.zstack.header.image.ImageArchitecture;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageDeletionMsg;
import org.zstack.header.image.ImageDeletionPolicyManager;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteFilesOnBackupStorageHostMsg;
import org.zstack.header.storage.backup.SoftwareUpgradePackageDeployMsg;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.CreateVmInstanceReply;
import org.zstack.header.vm.DestroyVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.managements.entity.common.ManagementNodeStatusView;
import org.zstack.managements.header.PremiumManagementsConstant;
import org.zstack.managements.header.h2.GetZSha2StatusMsg;
import org.zstack.managements.header.h2.GetZSha2StatusReply;
import org.zstack.accessKey.AccessKeyState;
import org.zstack.softwarePackage.compute.EstimatedImageSizeExtensionPoint;
import org.zstack.softwarePackage.compute.SoftwarePackageExtensionPoint;
import org.zstack.softwarePackage.compute.SoftwarePackageSystemTags;
import org.zstack.softwarePackage.compute.UploadSoftwarePackageToVmBackend;
import org.zstack.softwarePackage.compute.UploadSoftwarePackageToVmSpec;
import org.zstack.softwarePackage.compute.UploadSoftwarePackageToBackupStorageExtensionPoint;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.softwarePackage.entity.UpgradeType;
import org.zstack.softwarePackage.entity.UploadSoftwarePackageToBackupStorageLongJobData;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.TagUtils;
import org.zstack.utils.ShellResult;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.zmigrate.ZMigrateGlobalConfig;
import org.zstack.zmigrate.api.*;
import org.zstack.zmigrate.client.ZMigrateGatewayHelper;
import org.zstack.zmigrate.client.ZMigrateHttpClient;
import org.zstack.zmigrate.client.ZMigrateSshClient;
import org.zstack.zops.ChronyServerInfoPair;
import org.zstack.zops.GetChronyServersMsg;
import org.zstack.zops.GetChronyServersReply;
import org.zstack.zops.ZOpsConstants;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zstack.core.Platform.err;
import static org.zstack.core.config.schema.GuestOsCategory.getDefaultGuestOsTypeByPlatform;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.zmigrate.ZMigrateConstant.*;
import static org.zstack.zmigrate.ZMigrateGlobalConfig.*;
import static org.zstack.zmigrate.ZMigratePluginErrors.*;
import static org.zstack.zmigrate.ZMigrateSystemTags.*;
import static org.zstack.zmigrate.compute.ZMigrateUtils.*;
import static org.zstack.zmigrate.client.ZMigrateHttpClient.*;

public class ZMigrateManager extends AbstractService implements
        SoftwarePackageExtensionPoint
        , UploadSoftwarePackageToBackupStorageExtensionPoint
        , UploadSoftwarePackageToVmBackend
        , EstimatedImageSizeExtensionPoint
        , ManagementNodeReadyExtensionPoint
{
    private static final CLogger logger = Utils.getLogger(ZMigrateManager.class);
    static final int VDDK_DISTRIBUTION_CONCURRENCY = 3;

    @Autowired
    private CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;

    private Supplier<ZMigrateSshClient> sshClientFactory =
            () -> Platform.New(ZMigrateSshClient::new);
    private final AtomicBoolean vddkDistributionReady = new AtomicBoolean(false);
    private final AtomicBoolean vddkDistributionScanning = new AtomicBoolean(false);
    private VddkDistributionTask vddkDistributionTask;

    @Override
    public synchronized boolean start() {
        startVddkDistributionTask();
        ZMigrateGlobalConfig.VDDK_DISTRIBUTION_SCAN_INTERVAL.installUpdateExtension(
                (oldConfig, newConfig) -> startVddkDistributionTask());
        return true;
    }

    @Override
    public synchronized boolean stop() {
        vddkDistributionReady.set(false);
        if (vddkDistributionTask != null) {
            vddkDistributionTask.stop();
            vddkDistributionTask = null;
        }
        return true;
    }

    @Override
    public void managementNodeReady() {
        vddkDistributionReady.set(true);
        triggerVddkDistributionScan();
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            if (msg instanceof APIGetZMigrateInfosMsg) {
                handle((APIGetZMigrateInfosMsg) msg);
            } else if (msg instanceof APIGetZMigrateImagesMsg) {
                handle((APIGetZMigrateImagesMsg) msg);
            } else if (msg instanceof APIGetZMigrateGatewayVmInstancesMsg) {
                handle((APIGetZMigrateGatewayVmInstancesMsg) msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    // ------------------------------------- Message Handers ------------------------------------
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handle(APIGetZMigrateInfosMsg msg) {
        APIGetZMigrateInfosReply reply = new APIGetZMigrateInfosReply();
        String managementVmUuid = msg.getManagementVmUuid();

        VmInstanceState state = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, managementVmUuid)
                .select(VmInstanceVO_.state)
                .findValue();
        if (state == null) {
            reply.setError(err(MISSING_ZMIGRATE_VM, "ZMigrate management VM state not found"));
            bus.reply(msg, reply);
            return;
        }

        reply.setZmigrateVmInstanceStatus(state.toString());
        reply.setVddkUploaded(uploadedVddkTaskUuid(managementVmUuid) != null);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            bus.reply(msg, reply);
            return;
        }

        if (state != VmInstanceState.Running) {
            bus.reply(msg, reply);
            return;
        }

        ZMigrateHttpClient httpClient = createHttpClient();

        ErrorableValue<ZMigrateHttpClient.GetZMigrateInfoResponse> zMigrateManagementServerInfo = httpClient.getZMigrateManagementServerInfo(3, 10);
        if (!zMigrateManagementServerInfo.isSuccess()) {
            logger.warn(String.format("failed to get zmigrate management server info, error: %s", zMigrateManagementServerInfo.error.getDetails()));
        } else {
            reply.setVersion(zMigrateManagementServerInfo.result.version);
            Long timestamp = zMigrateManagementServerInfo.result.timestamp;
            Long uptime = zMigrateManagementServerInfo.result.uptime;
            if (timestamp != null && uptime != null) {
                reply.setZmigrateStartTime((timestamp - uptime) * 1000);
            } else {
                logger.warn(String.format("cannot compute zmigrateStartTime, timestamp=%s uptime=%s", timestamp, uptime));
            }
        }

        ErrorableValue<String> platformInfos = httpClient.getPlatformInfos();
        if (!platformInfos.isSuccess()) {
            logger.warn(String.format("failed to get platform infos, error: %s", platformInfos.error.getDetails()));
        } else {
            List<Map<String, Object>> platformInfoList = JSONObjectUtil.toObject(platformInfos.result, List.class);
            reply.setPlatformsCount(ZMigrateHttpClient.countValidPlatforms(platformInfoList));
        }

        ErrorableValue<String> gatewayServerInfos = httpClient.getGatewayServerInfos();
        if (!gatewayServerInfos.isSuccess()) {
            logger.warn(String.format("failed to get gateway server infos, error: %s", gatewayServerInfos.error.getDetails()));
        } else {
            List<Map<String, Object>> gatewayServerInfoList = JSONObjectUtil.toObject(gatewayServerInfos.result, List.class);
            reply.setGatewaysCount(ZMigrateHttpClient.countValidGateways(gatewayServerInfoList));
        }

        ErrorableValue<String> migrateJobs = httpClient.getMigrateJobs();
        if (!migrateJobs.isSuccess()) {
            logger.warn(String.format("failed to get migrate jobs, error: %s", migrateJobs.error.getDetails()));
        } else {
            List migrateJobList = JSONObjectUtil.toObject(migrateJobs.result, List.class);
            reply.setMigrateJobsCount(migrateJobList.size());
        }

        bus.reply(msg, reply);
    }

    private void handle(APIGetZMigrateImagesMsg msg) {
        APIGetZMigrateImagesReply reply = new APIGetZMigrateImagesReply();
        reply.setImages(getZMigrateImages());
        reply.getImages().putAll(getZMigrateUpgradeImages());
        bus.reply(msg, reply);
    }

    private void handle(APIGetZMigrateGatewayVmInstancesMsg msg) {
        APIGetZMigrateGatewayVmInstancesReply reply = new APIGetZMigrateGatewayVmInstancesReply();

        String managementVmUuid = msg.getManagementVmUuid();
        List<String> gatewayVmUuids = findZMigrateVmUuids(ZMIGRATE_GATEWAY);

        List<String> vmUuids = new ArrayList<>();
        if (managementVmUuid != null) {
            vmUuids.add(managementVmUuid);
        }
        vmUuids.addAll(gatewayVmUuids);
        if (vmUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        List<VmInstanceVO> vms = Q.New(VmInstanceVO.class).in(VmInstanceVO_.uuid, vmUuids).list();
        vms.forEach(vm -> reply.getGatewayVmInstances().add(VmInstanceInventory.valueOf(vm)));
        reply.setManagementVmInstanceUuid(managementVmUuid);
        bus.reply(msg, reply);
    }


    // ------------------------------------- SoftwarePackageExtensionPoint ------------------------------------

    @Override
    public String getSoftwarePackageType() {
        return ZMIGRATE_SOFTWARE_PACKAGE_TYPE;
    }

    @Override
    public boolean isInstalledAndUnmanagedByMn() {
        return false;
    }

    static class InstallZMigrateContext {
        String gatewayImageUuid;

        String mnIps;
        String platformUuidOnZMigrate;
        String mnIpToAddZMigrate;
        String vmUuid; // gateway VM UUID
        String accessKeyID;
        String accessKeySecret;
    }

    @Override
    public void installSoftwarePackage(SoftwarePackageVO softwarePackage, String config, SessionInventory session, Completion completion) {
        InstallZMigrateContext context = new InstallZMigrateContext();

        SimpleFlowChain.of("install-zmigrate")
        .then(Flow.of("find-gateway-image")
            .handle(trigger -> {
                context.gatewayImageUuid = ZMIGRATE_GATEWAY_IMAGE.getTokenByResourceUuid(softwarePackage.getUuid(), ZMIGRATE_GATEWAY_IMAGE_TOKEN);
                if (context.gatewayImageUuid == null) {
                    trigger.fail(err(INVALID_ZMIGRATE_TAGS, "can not find image %s for zmigrate gateway", ZMIGRATE_GATEWAY_IMAGE_TOKEN)
                            .withOpaque("image.uuid", context.gatewayImageUuid));
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("create-mn-gateway-vm")
            .handle(trigger -> {
                APICreateVmInstanceMsg amsg = JSONObjectUtil.toObject(config, APICreateVmInstanceMsg.class);
                amsg.setResourceUuid(ZMIGRATE_MANAGEMENT_VM_UUID);
                amsg.setSession(session);
                CreateVmInstanceMsg cvmsg = VmInstanceUtils.fromAPICreateVmInstanceMsg(amsg);
                List<String> systemTags = cvmsg.getSystemTags() == null ? new ArrayList<>() : new ArrayList<>(cvmsg.getSystemTags());
                systemTags.removeIf(tag -> Objects.equals(tag, ZMIGRATE_MANAGEMENT.getTagFormat()) || Objects.equals(tag, ZMIGRATE_GATEWAY.getTagFormat()));
                systemTags.add(ZMIGRATE_MANAGEMENT.getTagFormat());
                cvmsg.setSystemTags(systemTags);
                cvmsg.setImageUuid(context.gatewayImageUuid);
                bus.makeLocalServiceId(cvmsg, VmInstanceConstant.SERVICE_ID);
                bus.send(cvmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(err(GENERIC_ERROR, "create zmigrate management vm failed")
                                    .withCause(reply.getError()));
                            return;
                        }
                        CreateVmInstanceReply reply1 = reply.castReply();
                        context.vmUuid = reply1.getInventory().getUuid();
                        trigger.next();
                    }
                });
            })
            .rollback(trigger -> {
                if (context.vmUuid == null) {
                    trigger.rollback();
                    return;
                }

                DestroyVmInstanceMsg dmsg = new DestroyVmInstanceMsg();
                dmsg.setVmInstanceUuid(context.vmUuid);
                dmsg.setDeletionPolicy(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct);
                bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, context.vmUuid);
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to delete the vmInstance[uuid:%s]: %s",
                                    context.vmUuid, reply.getError().getReadableDetails()));
                        }
                        ZMIGRATE_MANAGEMENT.delete(context.vmUuid);
                        trigger.rollback();
                    }
                });
            })
            .build())
        .then(Flow.of("get-zsv-management-node-ips")
            .handle(trigger -> {
                GetZSha2StatusMsg gmsg = new GetZSha2StatusMsg();
                bus.makeLocalServiceId(gmsg, PremiumManagementsConstant.HA2_SERVICE_ID);
                bus.send(gmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(err(GENERIC_ERROR, "get zsv management node ips failed")
                                    .withCause(reply.getError()));
                            return;
                        }

                        GetZSha2StatusReply reply1 = reply.castReply();
                        context.mnIps = reply1.getInventory().getNodes().stream()
                                .map(ManagementNodeStatusView::getIp)
                                .collect(Collectors.joining(","));
                        context.mnIpToAddZMigrate = reply1.getInventory().getNodes().get(0).getIp();
                        if (reply1.getInventory().getVip() != null) {
                            context.mnIps += "," + reply1.getInventory().getVip();
                            context.mnIpToAddZMigrate = reply1.getInventory().getVip();
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .then(Flow.of("check-zmigrate-management-vm-is-ready")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> createSshClient().checkZMigrateManagementIsReady(context.mnIps, trigger.toCompletion()))
            .build())
        .then(Flow.of("configure-systemd-timesyncd")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> {
                GetChronyServersMsg gmsg = new GetChronyServersMsg();
                bus.makeLocalServiceId(gmsg, ZOpsConstants.SERVICE_ID);
                bus.send(gmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        GetChronyServersReply r = reply.castReply();
                        List<ChronyServerInfoPair> pairs = r.getServers();

                        List<String> ntpServers = new ArrayList<>();
                        if (pairs != null) {
                            for (ChronyServerInfoPair p : pairs) {
                                if (p == null) {
                                    continue;
                                }
                                if (p.getInternal() != null
                                        && StringUtils.isNotEmpty(p.getInternal().getHostname())) {
                                    ntpServers.add(p.getInternal().getHostname());
                                } else if (p.getExternal() != null
                                        && StringUtils.isNotEmpty(p.getExternal().getHostname())) {
                                    ntpServers.add(p.getExternal().getHostname());
                                }
                            }
                        }

                        if (ntpServers.isEmpty()) {
                            logger.warn("no chrony servers available, skip timesyncd configuration");
                            trigger.next();
                            return;
                        }

                        ErrorableValue<Boolean> result = createSshClient().configureTimesyncd(ntpServers);
                        if (!result.isSuccess()) {
                            trigger.fail(err(GENERIC_ERROR, "configure systemd-timesyncd failed")
                                    .withCause(result.error));
                            return;
                        }
                        trigger.next();
                    }
                });

            })
            .build())
        .then(Flow.of("overwrite-zmigrate-management-auth-addr")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> {
                final ErrorableValue<Boolean> rsp = createSshClient().overwriteZMigrateManagementAuthAddr();
                if (!rsp.isSuccess()) {
                    trigger.fail(err(GENERIC_ERROR, "overwrite zmigrate management auth_addr failed")
                            .withCause(rsp.error));
                    return;
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("check-zmigrate-management-server-is-ready")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> {
                ErrorableValue<ZMigrateHttpClient.GetZMigrateInfoResponse> rsp =
                        createHttpClient().getZMigrateManagementServerInfo(5, 30);
                if (!rsp.isSuccess()) {
                    trigger.fail(err(GENERIC_ERROR, "check zmigrate management server failed")
                            .withCause(rsp.error));
                    return;
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("create-zmigrate-user")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> {
                ErrorableValue<Boolean> rsp = createHttpClient().createAccount();
                if (!rsp.isSuccess()) {
                    trigger.fail(err(GENERIC_ERROR, "create zmigrate user failed").withCause(rsp.error));
                    return;
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("create-ak-if-needed")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> {
                List<Tuple> accessKeyVOs = Q.New(AccessKeyVO.class)
                        .eq(AccessKeyVO_.accountUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                        .eq(AccessKeyVO_.state, AccessKeyState.Enabled)
                        .select(AccessKeyVO_.AccessKeyID, AccessKeyVO_.AccessKeySecret)
                        .listTuple();

                if (!accessKeyVOs.isEmpty()) {
                    Tuple accessKeyVO = accessKeyVOs.get(0);
                    context.accessKeyID = accessKeyVO.get(0, String.class);
                    context.accessKeySecret = accessKeyVO.get(1, String.class);
                    trigger.next();
                    return;
                }

                CreateAccessKeyMsg msg = new CreateAccessKeyMsg();
                msg.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                msg.setUserUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                bus.makeLocalServiceId(msg, AccessKeyConstant.SERVICE_ID);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(err(GENERIC_ERROR, "create access key failed")
                                    .withCause(reply.getError()));
                            return;
                        }

                        CreateAccessKeyReply r = reply.castReply();
                        context.accessKeyID = r.getInventory().getAccessKeyID();
                        context.accessKeySecret = r.getInventory().getAccessKeySecret();
                        trigger.next();
                    }
                });
            })
            .build())
        // ZSvirt does not provide the ZStack license API used by ZMigrate's VerifyCloudConnection.
        /*.then(Flow.of("verify-zmigrate-platform-connection")
                .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
                .handle(trigger -> {
                    ErrorableValue<Boolean> rsp = createHttpClient().verifyPlatformConnection(
                            context.mnIpToAddZMigrate, context.accessKeyID, context.accessKeySecret);
                    if (!rsp.isSuccess()) {
                        trigger.fail(err(GENERIC_ERROR, "verify zmigrate platform connection failed")
                                .withCause(rsp.error));
                        return;
                    }
                    trigger.next();
                })
                .build())*/
        .then(Flow.of("register-zsv-to-zmigrate")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> {
                ErrorableValue<RegisterZsvToZMigratePlatformResponse> rsp = createHttpClient()
                        .registerZsvToZMigrate(context.mnIpToAddZMigrate, context.accessKeyID, context.accessKeySecret);
                if (!rsp.isSuccess()) {
                    trigger.fail(err(GENERIC_ERROR, "register zsv to zmigrate platform failed")
                            .withCause(rsp.error));
                    return;
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("get-zsv-platform-uuid-on-zmigrate-platform")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> {
                ErrorableValue<String> platformInfos = createHttpClient().getPlatformInfos();
                if (!platformInfos.isSuccess()) {
                    trigger.fail(err(GENERIC_ERROR, "get platform infos failed")
                            .withCause(platformInfos.error));
                    return;
                }

                List<Map<String, Object>> platformInfoList = JSONObjectUtil.toObject(platformInfos.result, List.class);
                platformInfoList.forEach((platformInfo -> {
                    Map authInfo = (Map) platformInfo.get(ZMIGRATE_PLATFORM_INFO_ON_CLOUD_AUTH_INFO_KEY);
                    if (Objects.isNull(authInfo)) {
                        return;
                    }
                    if (getZsvPlatformDisplayNameOnZMigrate().equals(authInfo.getOrDefault(ZSV_PLATFORM_DISPLAY_NAME_ON_ZMIGRATE_KEY, null))) {
                        Object platformUuidObj = platformInfo.get(ZMIGRATE_PLATFORM_INFO_ON_CLOUD_UUID_KEY);
                        if (Objects.nonNull(platformUuidObj)) {
                            context.platformUuidOnZMigrate = platformUuidObj.toString();
                        }
                    }
                }));

                if (context.platformUuidOnZMigrate == null) {
                    trigger.fail(err(GENERIC_ERROR, "zmigrate platform uuid on zmigrate not found"));
                    return;
                }

                trigger.next();
            })
            .build())
        .then(Flow.of("verify-zmigrate-gateway-connection")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> {
                ErrorableValue<Boolean> rsp = createHttpClient().verifyGatewayConnection();
                if (!rsp.isSuccess()) {
                    trigger.fail(err(GENERIC_ERROR, "verify zmigrate gateway connection failed")
                            .withCause(rsp.error));
                    return;
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("register-zmigrate-gateway")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> {
                ErrorableValue<Boolean> rsp = createHttpClient().registerGatewayToZMigrate(context.platformUuidOnZMigrate);
                if (!rsp.isSuccess()) {
                    trigger.fail(err(GENERIC_ERROR, "register zmigrate gateway to platform failed").withCause(rsp.error));
                    return;
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("get-zmigrate-license-key")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> {
                ErrorableValue<String> rsp = createHttpClient().exportActivationInfos();
                if (!rsp.isSuccess()) {
                    trigger.fail(err(GENERIC_ERROR, "failed to get zmigrate license key").withCause(rsp.error));
                    return;
                }

                SystemTagCreator creator = ZMIGRATE_LICENSE_KEY.newSystemTagCreator(softwarePackage.getUuid());
                creator.inherent = false;
                creator.recreate = true;
                creator.setTagByTokens(map(e(ZMIGRATE_LICENSE_KEY_TOKEN, rsp.result)));
                creator.create();
                trigger.next();
            })
            .build())
        .propagateExceptionTo(completion)
        .done(completion::success)
        .error(completion::fail)
        .start();
    }

    @Override
    public void uninstallSoftwarePackage(SoftwarePackageVO softwarePackage, Completion completion) {
        String backupStorageUuid = SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID
                    .getTokenByResourceUuid(softwarePackage.getUuid(), SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN);

        SimpleFlowChain.of("uninstall-zmigrate")
        .then(Flow.of("delete-zmigrate-vms")
            .handle(trigger -> {
                LinkedHashSet<String> vmUuidSet = new LinkedHashSet<>();
                vmUuidSet.addAll(findZMigrateVmUuids(ZMIGRATE_MANAGEMENT));
                vmUuidSet.addAll(findZMigrateVmUuids(ZMIGRATE_GATEWAY));
                if (vmUuidSet.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(new ArrayList<>(vmUuidSet)).step((vmUuid, whileCompletion) -> {
                    DestroyVmInstanceMsg dmsg = new DestroyVmInstanceMsg();
                    dmsg.setVmInstanceUuid(vmUuid);
                    dmsg.setDeletionPolicy(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct);
                    bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, vmUuid);
                    bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileCompletion.addError(reply.getError());
                            }
                            whileCompletion.done();
                        }
                    });
                }, 3).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(err(GENERIC_ERROR, "failed to delete zmigrate VM").withCause(errorCodeList));
                            return;
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .then(Flow.of("delete-zmigrate-images")
            .skipIf(data -> backupStorageUuid == null)
            .handle(trigger -> {
                String spUuid = softwarePackage.getUuid();
                String gatewayImageUuid = getTokenFromTag(ZMIGRATE_GATEWAY_IMAGE, ZMIGRATE_GATEWAY_IMAGE_TOKEN, spUuid);
                String linuxBootImageUuid = getTokenFromTag(ZMIGRATE_LINUX_BOOT_IMAGE, ZMIGRATE_LINUX_BOOT_IMAGE_TOKEN, spUuid);
                String windowsBootImageUuid = getTokenFromTag(ZMIGRATE_WINDOWS_BOOT_IMAGE, ZMIGRATE_WINDOWS_BOOT_IMAGE_TOKEN, spUuid);
                String gatewayUpgradeImageUuid = getTokenFromTag(ZMIGRATE_GATEWAY_UPGRADE_IMAGE, ZMIGRATE_GATEWAY_UPGRADE_IMAGE_TOKEN, spUuid);
                String linuxBootUpgradeImageUuid = getTokenFromTag(ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE, ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE_TOKEN, spUuid);
                String windowsBootUpgradeImageUuid = getTokenFromTag(ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE, ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE_TOKEN, spUuid);

                List<String> imageUuids = Stream.of(
                        gatewayImageUuid, linuxBootImageUuid, windowsBootImageUuid,
                        gatewayUpgradeImageUuid, linuxBootUpgradeImageUuid, windowsBootUpgradeImageUuid
                        ).filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (imageUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(imageUuids).step((imageUuid, whileCompletion) -> {
                    ImageDeletionMsg dmsg = new ImageDeletionMsg();
                    dmsg.setImageUuid(imageUuid);
                    dmsg.setForceDelete(true);
                    dmsg.setBackupStorageUuids(list(backupStorageUuid));
                    dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, imageUuid);
                    bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileCompletion.addError(reply.getError());
                            }
                            whileCompletion.done();
                        }
                    });
                }, 3).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(err(GENERIC_ERROR, "failed to delete zmigrate images")
                                    .withCause(errorCodeList));
                            return;
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .propagateExceptionTo(completion)
        .done(completion::success)
        .error(completion::fail)
        .start();
    }

    @Override
    public void cleanSoftwarePackage(SoftwarePackageVO softwarePackage, Completion completion) {
        final String backupStorageUuid = SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID
                .getTokenByResourceUuid(softwarePackage.getUuid(), SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN);
        final String backupStorageHostUuid = SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID
                .getTokenByResourceUuid(softwarePackage.getUuid(), SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN);

        SimpleFlowChain.of("clean-zmigrate")
        .then(Flow.of("delete-zmigrate-images")
            .skipIf(data -> backupStorageUuid == null)
            .handle(trigger -> {
                String spUuid = softwarePackage.getUuid();
                String gatewayImageUuid = getTokenFromTag(ZMIGRATE_GATEWAY_IMAGE, ZMIGRATE_GATEWAY_IMAGE_TOKEN, spUuid);
                String linuxBootImageUuid = getTokenFromTag(ZMIGRATE_LINUX_BOOT_IMAGE, ZMIGRATE_LINUX_BOOT_IMAGE_TOKEN, spUuid);
                String windowsBootImageUuid = getTokenFromTag(ZMIGRATE_WINDOWS_BOOT_IMAGE, ZMIGRATE_WINDOWS_BOOT_IMAGE_TOKEN, spUuid);

                List<String> imageUuids = new ArrayList<>();
                if (gatewayImageUuid != null) {
                    imageUuids.add(gatewayImageUuid);
                }
                if (linuxBootImageUuid != null) {
                    imageUuids.add(linuxBootImageUuid);
                }
                if (windowsBootImageUuid != null) {
                    imageUuids.add(windowsBootImageUuid);
                }
                if (imageUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(imageUuids).step((imageUuid, whileCompletion) -> {
                    ImageDeletionMsg dmsg = new ImageDeletionMsg();
                    dmsg.setImageUuid(imageUuid);
                    dmsg.setForceDelete(true);
                    dmsg.setBackupStorageUuids(list(backupStorageUuid));
                    dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, imageUuid);
                    bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileCompletion.addError(reply.getError());
                            }
                            whileCompletion.done();
                        }
                    });
                }, 3).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(err(GENERIC_ERROR, "failed to delete zmigrate images")
                                    .withCause(errorCodeList));
                            return;
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .then(Flow.of("delete-package-on-backup-storage-host")
            .skipIf(data -> {
                if (backupStorageUuid == null || backupStorageHostUuid == null) {
                    return true;
                }
                if (softwarePackage.getInstallPath() == null && softwarePackage.getUnzipInstallPath() == null) {
                    logger.warn(String.format("skip delete-package-on-backup-storage-host: both installPath and unzipInstallPath are null for software package [uuid:%s]",
                            softwarePackage.getUuid()));
                    return true;
                }
                return false;
            })
            .handle(trigger -> {
                DeleteFilesOnBackupStorageHostMsg dmsg = new DeleteFilesOnBackupStorageHostMsg();
                dmsg.setBackupStorageUuid(backupStorageUuid);
                dmsg.setBackupStorageHostUuid(backupStorageHostUuid);
                if (softwarePackage.getInstallPath() != null) {
                    dmsg.getFilePaths().add(softwarePackage.getInstallPath());
                }
                if (softwarePackage.getUnzipInstallPath() != null) {
                    dmsg.getFilePaths().add(softwarePackage.getUnzipInstallPath());
                }
                bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, dmsg.getBackupStorageUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("delete files %s on backup storage host %s failed, error: %s",
                                    dmsg.getFilePaths(), dmsg.getBackupStorageHostUuid(), reply.getError().getDetails()));
                            trigger.fail(reply.getError());
                            return;
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .propagateExceptionTo(completion)
        .done(completion::success)
        .error(completion::fail)
        .start();
    }

    // ------------------------------------- UploadSoftwarePackageToBackupStorageExtensionPoint ------------------------------------

    static class UpgradeZMigrateContext {
        final Map<String, ImageInventory> zMigrateImages = getZMigrateImages();
        String gatewayUpgradeImageUuid;
        String linuxBootUpgradeImageUuid;
        String windowsBootUpgradeImageUuid;
        boolean upgradeFailed = false;
        List<String> upgradeImageUuids = new ArrayList<>();
    }

    @Override
    public void upgradeSoftwarePackage(SoftwarePackageVO softwarePackage, UploadSoftwarePackageToBackupStorageLongJobData msgData, Completion completion) {
        UpgradeZMigrateContext context = new UpgradeZMigrateContext();

        SimpleFlowChain.of("upgrade-software-package")
        .then(Flow.of("setup")
            .handle(trigger -> {
                if (msgData.backupStorageUuid == null) {
                    msgData.backupStorageUuid = SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID
                            .getTokenByResourceUuid(msgData.softwarePackageUuid, SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN);
                }

                if (msgData.backupStorageHostUuid == null) {
                    msgData.backupStorageHostUuid = SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID
                            .getTokenByResourceUuid(msgData.softwarePackageUuid, SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN);
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("determined-zmigrate-image")
            .runIf(data -> Objects.equals(msgData.upgradeType, UpgradeType.Reexecute.toString()))
            .handle(trigger -> {
                final Map<String, ImageInventory> upgradeImages = getZMigrateUpgradeImages();
                upgradeImages.forEach((k, v) -> {
                    switch (k) {
                    case ZMIGRATE_GATEWAY_UPGRADE_IMAGE_TOKEN:
                        context.gatewayUpgradeImageUuid = v.getUuid();
                        break;
                    case ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE_TOKEN:
                        context.linuxBootUpgradeImageUuid = v.getUuid();
                        break;
                    case ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE_TOKEN:
                        context.windowsBootUpgradeImageUuid = v.getUuid();
                        break;
                    }
                });
                trigger.next();
            })
            .build())
        .then(Flow.of("add-upgrade-images")
            .skipIf(data -> Objects.equals(msgData.upgradeType, UpgradeType.Reexecute.toString()))
            .handle(trigger -> {
                new While<>(msgData.imagesPath).each((imagePath, whileCompletion) -> {
                    String imageName = PathUtil.fileName(imagePath);

                    AddImageMsg amsg = new AddImageMsg();
                    amsg.setSession(msgData.session);
                    amsg.setResourceUuid(Platform.getUuid());
                    amsg.setName(imageName);
                    amsg.setBackupStorageUuids(list(msgData.backupStorageUuid));
                    amsg.setFormat(ImageConstant.QCOW2_FORMAT_STRING);
                    amsg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
                    amsg.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
                    amsg.setVirtio(true);
                    amsg.setUrl(String.format("file://%s", imagePath));
                    amsg.setArchitecture(ImageArchitecture.x86_64.toString());
                    amsg.setDescription(imageName.startsWith(GATEWAY_IMAGE_PREFIX) ? GATEWAY_IMAGE_DESCRIPTION : BOOT_IMAGE_DESCRIPTION);

                    final PatternedSystemTag upgradeImageSystemTag;
                    final String upgradeImageTokenName;
                    if (imageName.startsWith(GATEWAY_IMAGE_PREFIX)) {
                        upgradeImageSystemTag = ZMIGRATE_GATEWAY_UPGRADE_IMAGE;
                        upgradeImageTokenName = ZMIGRATE_GATEWAY_UPGRADE_IMAGE_TOKEN;
                        amsg.setPlatform(ImagePlatform.Linux.toString());
                        amsg.setGuestOsType(getDefaultGuestOsTypeByPlatform(ImagePlatform.Linux.toString()));
                    } else if (imageName.startsWith(LINUX_BOOT_IMAGE_PREFIX)) {
                        upgradeImageSystemTag = ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE;
                        upgradeImageTokenName = ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE_TOKEN;
                        amsg.setPlatform(ImagePlatform.Linux.toString());
                        amsg.setGuestOsType(getDefaultGuestOsTypeByPlatform(ImagePlatform.Linux.toString()));
                    } else if (imageName.startsWith(WINDOWS_BOOT_IMAGE_PREFIX)) {
                        upgradeImageSystemTag = ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE;
                        upgradeImageTokenName = ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE_TOKEN;
                        amsg.setPlatform(ImagePlatform.Windows.toString());
                        amsg.setGuestOsType(getDefaultGuestOsTypeByPlatform(ImagePlatform.Windows.toString()));
                    } else {
                        logger.warn(String.format("unknown upgrade image type for image: %s, skipping", imageName));
                        whileCompletion.done();
                        return;
                    }

                    bus.makeLocalServiceId(amsg, ImageConstant.SERVICE_ID);
                    bus.send(amsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileCompletion.addError(reply.getError());
                                whileCompletion.done();
                                return;
                            }
                            AddImageReply r = reply.castReply();
                            String imageUuid = r.getInventory().getUuid();
                            context.upgradeImageUuids.add(imageUuid);
                            if (imageName.startsWith(GATEWAY_IMAGE_PREFIX)) {
                                context.gatewayUpgradeImageUuid = imageUuid;
                            } else if (imageName.startsWith(LINUX_BOOT_IMAGE_PREFIX)) {
                                context.linuxBootUpgradeImageUuid = imageUuid;
                            } else {
                                context.windowsBootUpgradeImageUuid = imageUuid;
                            }
                            SystemTagCreator creator = upgradeImageSystemTag.newSystemTagCreator(softwarePackage.getUuid());
                            creator.inherent = false;
                            creator.recreate = true;
                            creator.setTagByTokens(map(e(upgradeImageTokenName, r.getInventory().getUuid())));
                            creator.create();
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(err(GENERIC_ERROR, "failed to add zmigrate images")
                                    .withCause(errorCodeList));
                            return;
                        }
                        trigger.next();
                    }
                });

            })
            .rollback(trigger -> {
                if (context.upgradeImageUuids.isEmpty() || context.upgradeFailed) {
                    trigger.rollback();
                    return;
                }

                for (String imageUuid : context.upgradeImageUuids) {
                    if (imageUuid.equals(context.gatewayUpgradeImageUuid)) {
                        ZMIGRATE_GATEWAY_UPGRADE_IMAGE.delete(softwarePackage.getUuid());
                    }
                    if (imageUuid.equals(context.linuxBootUpgradeImageUuid)) {
                        ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE.delete(softwarePackage.getUuid());
                    }
                    if (imageUuid.equals(context.windowsBootUpgradeImageUuid)) {
                        ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE.delete(softwarePackage.getUuid());
                    }
                }

                for (String imageUuid : context.upgradeImageUuids) {
                    ImageDeletionMsg dmsg = new ImageDeletionMsg();
                    dmsg.setImageUuid(imageUuid);
                    dmsg.setBackupStorageUuids(list(msgData.backupStorageUuid));
                    dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, imageUuid);
                    bus.send(dmsg, new CloudBusCallBack(dmsg) {
                        @Override
                        public void run(MessageReply reply) {
                            logger.info(String.format("delete image %s: %s", imageUuid, reply.isSuccess()));
                        }
                    });
                }
                trigger.rollback();
            })
            .build())
        .then(Flow.of("deploy-and-execute-upgrade-package-on-gateway-vm")
            .handle(trigger -> {
                SoftwareUpgradePackageDeployMsg dmsg = new SoftwareUpgradePackageDeployMsg();
                dmsg.setBackupStorageUuid(msgData.backupStorageUuid);
                dmsg.setBackupStorageHostUuid(msgData.backupStorageHostUuid);
                dmsg.setUpgradePackagePath(msgData.upgradePackagePath);
                String pathForFileName = msgData.installPath != null ? msgData.installPath : msgData.upgradePackagePath;
                if (pathForFileName == null) {
                    trigger.fail(err(GENERIC_ERROR, "both installPath and upgradePackagePath are null, cannot determine upgrade package file name"));
                    return;
                }
                String fileName = PathUtil.fileName(pathForFileName);
                String upgradePackageTargetPath = String.format("%s/%s_%d", UPGRADE_PACKAGE_ROOT_DIR, fileName, System.currentTimeMillis());
                dmsg.setUpgradePackageTargetPath(upgradePackageTargetPath);

                dmsg.setTargetHostSshPort(GATEWAY_SSH_PORT);
                dmsg.setTargetHostSshUsername(GATEWAY_SSH_USERNAME);
                dmsg.setTargetHostSshPassword(ZMigrateGlobalConfig.GATEWAY_SSH_PASSWORD.value());
                ErrorableValue<String> gatewayManagementIp = ZMigrateGatewayHelper.getGatewayManagementIp();
                if (!gatewayManagementIp.isSuccess()) {
                    trigger.fail(err(GENERIC_ERROR, "failed to get gateway management IP")
                            .withCause(gatewayManagementIp.error));
                    return;
                }
                dmsg.setTargetHostIp(gatewayManagementIp.result);
                dmsg.setUpgradeScriptPath(String.format("%s%s", dmsg.getUpgradePackageTargetPath(), UPGRADE_SCRIPT_PATH));
                dmsg.setSoftwareType(ZMIGRATE_SOFTWARE_PACKAGE_TYPE);

                bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            context.upgradeFailed = true;
                            trigger.fail(err(GENERIC_ERROR,
                                    "failed to deploy and execute ZMigrate upgrade package on management node vm [ip:%s, sshPort:%d, script:%s]",
                                    dmsg.getTargetHostIp(), dmsg.getTargetHostSshPort(), dmsg.getUpgradeScriptPath())
                                            .withCause(reply.getError()));
                            return;
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .then(Flow.of("update-image-system-tag")
            .handle(trigger -> {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        doDeleteImageSystemTag(ZMIGRATE_GATEWAY_UPGRADE_IMAGE);
                        doDeleteImageSystemTag(ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE);
                        doDeleteImageSystemTag(ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE);
                        context.zMigrateImages.forEach((k, v) -> {
                            switch (k) {
                                case ZMIGRATE_GATEWAY_IMAGE_TOKEN:
                                    doDeleteImageSystemTag(ZMIGRATE_GATEWAY_IMAGE);
                                    break;
                                case ZMIGRATE_LINUX_BOOT_IMAGE_TOKEN:
                                    doDeleteImageSystemTag(ZMIGRATE_LINUX_BOOT_IMAGE);
                                    break;
                                case ZMIGRATE_WINDOWS_BOOT_IMAGE_TOKEN:
                                    doDeleteImageSystemTag(ZMIGRATE_WINDOWS_BOOT_IMAGE);
                                    break;
                            }
                        });
                    }

                    private void doDeleteImageSystemTag(PatternedSystemTag systemtag) {
                        sql(SystemTagVO.class)
                                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(systemtag.getTagFormat()))
                                .eq(SystemTagVO_.resourceType, SoftwarePackageVO.class.getSimpleName())
                                .eq(SystemTagVO_.resourceUuid, softwarePackage.getUuid())
                                .delete();
                    }
                }.execute();

                if (context.gatewayUpgradeImageUuid == null || context.linuxBootUpgradeImageUuid == null || context.windowsBootUpgradeImageUuid == null) {
                    trigger.fail(err(GENERIC_ERROR, "upgrade images are incomplete, cannot update ZMigrate image system tags"));
                    return;
                }

                SystemTagCreator creator = ZMIGRATE_GATEWAY_IMAGE.newSystemTagCreator(softwarePackage.getUuid());
                creator.inherent = false;
                creator.recreate = true;
                creator.setTagByTokens(map(e(ZMIGRATE_GATEWAY_IMAGE_TOKEN, context.gatewayUpgradeImageUuid)));
                creator.create();

                creator = ZMIGRATE_LINUX_BOOT_IMAGE.newSystemTagCreator(softwarePackage.getUuid());
                creator.inherent = false;
                creator.recreate = true;
                creator.setTagByTokens(map(e(ZMIGRATE_LINUX_BOOT_IMAGE_TOKEN, context.linuxBootUpgradeImageUuid)));
                creator.create();

                creator = ZMIGRATE_WINDOWS_BOOT_IMAGE.newSystemTagCreator(softwarePackage.getUuid());
                creator.inherent = false;
                creator.recreate = true;
                creator.setTagByTokens(map(e(ZMIGRATE_WINDOWS_BOOT_IMAGE_TOKEN, context.windowsBootUpgradeImageUuid)));
                creator.create();

                trigger.next();
            })
            .build())
        .then(Flow.of("delete-old-image")
            .handle(trigger -> {
                new While<>(context.zMigrateImages.values()).all((image, whileCompletion) -> {
                    String imageUuid = image.getUuid();
                    ImageDeletionMsg dmsg = new ImageDeletionMsg();
                    dmsg.setImageUuid(imageUuid);
                    dmsg.setBackupStorageUuids(list(msgData.backupStorageUuid));
                    dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, imageUuid);
                    bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("failed to delete image %s on backup storage host %s",
                                        imageUuid, msgData.backupStorageHostUuid));
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            })
            .build())
        .then(Flow.of("delete-upgrade-package-on-backup-storage-host")
            .handle(trigger -> {
                DeleteFilesOnBackupStorageHostMsg dmsg = new DeleteFilesOnBackupStorageHostMsg();
                dmsg.setBackupStorageUuid(msgData.backupStorageUuid);
                dmsg.setBackupStorageHostUuid(msgData.backupStorageHostUuid);
                if (msgData.installPath != null) {
                    dmsg.getFilePaths().add(msgData.installPath);
                }
                if (msgData.unzipInstallPath != null) {
                    dmsg.getFilePaths().add(msgData.unzipInstallPath);
                }
                bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to delete files [%s, %s] on backup storage host %s",
                                    msgData.installPath, msgData.unzipInstallPath, msgData.backupStorageHostUuid));
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .propagateExceptionTo(completion)
        .done(completion::success)
        .error(completion::fail)
        .start();
    }

    @Override
    public Map<String, Long> getImagesSize(Map<String, Long> fileSizes) {
        Map<String, Long> imagesSize = new HashMap<>();
        if (fileSizes == null || fileSizes.isEmpty()) {
            return imagesSize;
        }
        fileSizes.forEach((filePath, size) -> {
            String fileName = PathUtil.fileName(filePath);
            if (fileName.startsWith(GATEWAY_IMAGE_PREFIX) || fileName.startsWith(LINUX_BOOT_IMAGE_PREFIX) || fileName.startsWith(WINDOWS_BOOT_IMAGE_PREFIX)) {
                imagesSize.put(filePath, size);
            }
        });
        return imagesSize;
    }

    @Override
    public String getUpgradePackagePath(Map<String, Long> fileSizes) {
        if (fileSizes == null || fileSizes.isEmpty()) {
            return null;
        }
        List<String> filesPath = new ArrayList<>(fileSizes.keySet());
        String upgradePackagePath = null;
        for (String filePath : filesPath) {
            String fileName = PathUtil.fileName(filePath);
            if (fileName.startsWith(UPGRADE_PACKAGE_PREFIX)) {
                upgradePackagePath = filePath;
                break;
            }
        }
        return upgradePackagePath;
    }

    static class AddImagesToBsContext {
        String gatewayImageUuid;
        String linuxBootImageUuid;
        String windowsBootImageUuid;
        List<ImageInventory> images = new ArrayList<>();
    }

    @Override
    public void afterUploadSoftwarePackageToBackupStorage(SoftwarePackageVO softwarePackage, UploadSoftwarePackageToBackupStorageLongJobData msgData, Completion completion) {
        AddImagesToBsContext context = new AddImagesToBsContext();

        SimpleFlowChain.of("add-images-to-backup-storage")
        .then(Flow.of("add-images-to-backup-storage")
            .handle(trigger -> {
                new While<>(msgData.imagesPath).each((imagePath, whileCompletion) -> {
                    String imageName = PathUtil.fileName(imagePath);
                    if (!imageName.startsWith(GATEWAY_IMAGE_PREFIX) && !imageName.startsWith(LINUX_BOOT_IMAGE_PREFIX) && !imageName.startsWith(WINDOWS_BOOT_IMAGE_PREFIX)) {
                        whileCompletion.done();
                        return;
                    }

                    AddImageMsg amsg = new AddImageMsg();
                    amsg.setSession(msgData.session);
                    amsg.setResourceUuid(Platform.getUuid());
                    amsg.setName(imageName);
                    amsg.setBackupStorageUuids(list(msgData.backupStorageUuid));
                    amsg.setFormat(ImageConstant.QCOW2_FORMAT_STRING);
                    amsg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
                    amsg.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
                    amsg.setVirtio(true);
                    amsg.setUrl(String.format("file://%s", imagePath));
                    amsg.setArchitecture(ImageArchitecture.x86_64.toString());
                    amsg.setDescription(imageName.startsWith(GATEWAY_IMAGE_PREFIX) ? GATEWAY_IMAGE_DESCRIPTION : BOOT_IMAGE_DESCRIPTION);
                    final PatternedSystemTag imageSystemTag;
                    final String imageTokenName;
                    if (imageName.startsWith(GATEWAY_IMAGE_PREFIX)) {
                        imageSystemTag = ZMIGRATE_GATEWAY_IMAGE;
                        imageTokenName = ZMIGRATE_GATEWAY_IMAGE_TOKEN;
                        amsg.setPlatform(ImagePlatform.Linux.toString());
                        amsg.setGuestOsType(getDefaultGuestOsTypeByPlatform(ImagePlatform.Linux.toString()));
                    } else if (imageName.startsWith(LINUX_BOOT_IMAGE_PREFIX)) {
                        imageSystemTag = ZMIGRATE_LINUX_BOOT_IMAGE;
                        imageTokenName = ZMIGRATE_LINUX_BOOT_IMAGE_TOKEN;
                        amsg.setPlatform(ImagePlatform.Linux.toString());
                        amsg.setGuestOsType(getDefaultGuestOsTypeByPlatform(ImagePlatform.Linux.toString()));
                    } else {
                        imageSystemTag = ZMIGRATE_WINDOWS_BOOT_IMAGE;
                        imageTokenName = ZMIGRATE_WINDOWS_BOOT_IMAGE_TOKEN;
                        amsg.setPlatform(ImagePlatform.Windows.toString());
                        amsg.setGuestOsType(getDefaultGuestOsTypeByPlatform(ImagePlatform.Windows.toString()));
                    }
                    bus.makeLocalServiceId(amsg, ImageConstant.SERVICE_ID);
                    bus.send(amsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileCompletion.addError(reply.getError());
                                whileCompletion.done();
                                return;
                            }
                            AddImageReply r = reply.castReply();
                            context.images.add(r.getInventory());
                            String imageUuid = r.getInventory().getUuid();
                            if (imageName.startsWith(GATEWAY_IMAGE_PREFIX)) {
                                context.gatewayImageUuid = imageUuid;
                            } else if (imageName.startsWith(LINUX_BOOT_IMAGE_PREFIX)) {
                                context.linuxBootImageUuid = imageUuid;
                            } else {
                                context.windowsBootImageUuid = imageUuid;
                            }
                            SystemTagCreator creator = imageSystemTag.newSystemTagCreator(softwarePackage.getUuid());
                            creator.inherent = false;
                            creator.recreate = true;
                            creator.setTagByTokens(map(e(imageTokenName, imageUuid)));
                            creator.create();
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(err(GENERIC_ERROR, "failed to add zmigrate images")
                                    .withCause(errorCodeList));
                            return;
                        }
                        trigger.next();
                    }
                });
            })
            .rollback(trigger -> {
                if (context.images.isEmpty()) {
                    trigger.rollback();
                    return;
                }

                for (ImageInventory image : context.images) {
                    String imageUuid = image.getUuid();
                    if (imageUuid.equals(context.gatewayImageUuid)) {
                        ZMIGRATE_GATEWAY_IMAGE.delete(softwarePackage.getUuid());
                    }
                    if (imageUuid.equals(context.linuxBootImageUuid)) {
                        ZMIGRATE_LINUX_BOOT_IMAGE.delete(softwarePackage.getUuid());
                    }
                    if (imageUuid.equals(context.windowsBootImageUuid)) {
                        ZMIGRATE_WINDOWS_BOOT_IMAGE.delete(softwarePackage.getUuid());
                    }

                    ImageDeletionMsg dmsg = new ImageDeletionMsg();
                    dmsg.setImageUuid(imageUuid);
                    dmsg.setBackupStorageUuids(list(msgData.backupStorageUuid));
                    dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, imageUuid);
                    bus.send(dmsg, new CloudBusCallBack(dmsg) {
                        @Override
                        public void run(MessageReply reply) {
                            logger.info(String.format("delete image %s: %s", imageUuid, reply.isSuccess()));
                        }
                    });
                }
                trigger.rollback();
            })
            .build())
        .then(Flow.of("delete-files-on-backup-storage-host")
            .handle(trigger -> {
                DeleteFilesOnBackupStorageHostMsg dmsg = new DeleteFilesOnBackupStorageHostMsg();
                dmsg.setBackupStorageUuid(msgData.backupStorageUuid);
                dmsg.setBackupStorageHostUuid(msgData.backupStorageHostUuid);
                if (msgData.installPath != null) {
                    dmsg.getFilePaths().add(msgData.installPath);
                }
                if (msgData.unzipInstallPath != null) {
                    dmsg.getFilePaths().add(msgData.unzipInstallPath);
                }
                bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
                bus.send(dmsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to delete files [%s, %s] on backup storage host %s",
                                    msgData.installPath, msgData.unzipInstallPath, msgData.backupStorageHostUuid));
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .propagateExceptionTo(completion)
        .done(completion::success)
        .error(completion::fail)
        .start();
    }

    @Override
    public void cleanUpgradeSoftwarePackage(SoftwarePackageVO softwarePackage, String backupStorageUuid, String backupStorageHostUuid, String installPath, String unzipInstallPath, Completion completion) {
        SimpleFlowChain.of("clean-upgrade-software-package")
        .then(Flow.of("delete-upgrade-images")
            .skipIf(data -> backupStorageUuid == null)
            .handle(trigger -> {
                String spUuid = softwarePackage.getUuid();
                String gatewayImageUuid = getTokenFromTag(ZMIGRATE_GATEWAY_UPGRADE_IMAGE, ZMIGRATE_GATEWAY_UPGRADE_IMAGE_TOKEN, spUuid);
                String linuxBootImageUuid = getTokenFromTag(ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE, ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE_TOKEN, spUuid);
                String windowsBootImageUuid = getTokenFromTag(ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE, ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE_TOKEN, spUuid);

                List<String> imageUuids = new ArrayList<>();
                if (gatewayImageUuid != null) {
                    imageUuids.add(gatewayImageUuid);
                }
                if (linuxBootImageUuid != null) {
                    imageUuids.add(linuxBootImageUuid);
                }
                if (windowsBootImageUuid != null) {
                    imageUuids.add(windowsBootImageUuid);
                }
                if (imageUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(imageUuids).all((imageUuid, whileCompletion) -> {
                    ImageDeletionMsg dmsg = new ImageDeletionMsg();
                    dmsg.setImageUuid(imageUuid);
                    dmsg.setForceDelete(true);
                    dmsg.setBackupStorageUuids(list(backupStorageUuid));
                    dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, imageUuid);
                    bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileCompletion.addError(reply.getError());
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(err(GENERIC_ERROR, "failed to delete zmigrate images")
                                    .withCause(errorCodeList));
                            return;
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .then(Flow.of("delete-upgrade-package-on-backup-storage-host")
            .skipIf(data -> backupStorageUuid == null || backupStorageHostUuid == null)
            .handle(trigger -> {
                DeleteFilesOnBackupStorageHostMsg dmsg = new DeleteFilesOnBackupStorageHostMsg();
                dmsg.setBackupStorageUuid(backupStorageUuid);
                dmsg.setBackupStorageHostUuid(backupStorageHostUuid);
                if (installPath != null) {
                    dmsg.getFilePaths().add(installPath);
                }
                if (unzipInstallPath != null) {
                    dmsg.getFilePaths().add(unzipInstallPath);
                }
                bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, backupStorageUuid);
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to delete upgrade package %s on backup storage host %s",
                                    installPath, backupStorageUuid));
                            trigger.fail(reply.getError());
                            return;
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .propagateExceptionTo(completion)
        .done(completion::success)
        .error(completion::fail)
        .start();
    }

    // ------------------------------------- UploadSoftwarePackageToVmBackend ------------------------------------

    @Override
    public String getType() {
        return ZMIGRATE_SOFTWARE_PACKAGE_TYPE;
    }

    @Override
    public ErrorCode validateTargetVm(VmInstanceInventory vm) {
        try {
            String managementVmUuid = findZMigrateVmUuid(ZMIGRATE_MANAGEMENT);
            if (managementVmUuid == null) {
                return err(MISSING_ZMIGRATE_VM, "no ZMigrate management VM found");
            }
            if (!Objects.equals(managementVmUuid, vm.getUuid())) {
                return err(INVALID_ZMIGRATE_TAGS,
                        "VM instance[uuid:%s] is not the ZMigrate management VM", vm.getUuid());
            }

            String softwarePackageUuid = getSoftwarePackageUuid();
            String gatewayImageUuid = getTokenFromTag(ZMIGRATE_GATEWAY_IMAGE,
                    ZMIGRATE_GATEWAY_IMAGE_TOKEN, softwarePackageUuid);
            if (gatewayImageUuid == null) {
                return err(INVALID_ZMIGRATE_TAGS, "ZMigrate gateway image is not configured");
            }
            if (!Objects.equals(gatewayImageUuid, vm.getImageUuid())) {
                return err(INVALID_ZMIGRATE_TAGS,
                        "ZMigrate management VM[uuid:%s] image[uuid:%s] does not match gateway image[uuid:%s]",
                        vm.getUuid(), vm.getImageUuid(), gatewayImageUuid);
            }
            return null;
        } catch (OperationFailureException e) {
            return e.getErrorCode();
        }
    }

    @Override
    public UploadSoftwarePackageToVmSpec getUploadSpec(String uploadTaskUuid) {
        UploadSoftwarePackageToVmSpec spec = new UploadSoftwarePackageToVmSpec();
        spec.setTargetPath(vddkUploadPath(uploadTaskUuid));
        spec.setUsername(GATEWAY_SSH_USERNAME);
        spec.setSshPort(GATEWAY_SSH_PORT);
        spec.setPassword(getGatewaySshPassword());
        return spec;
    }

    private String getGatewaySshPassword() {
        byte[] password = Base64.getDecoder().decode(ZMigrateGlobalConfig.GATEWAY_SSH_PASSWORD.value());
        try {
            return new String(password, java.nio.charset.StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(password, (byte) 0);
        }
    }

    @Override
    public void install(String vmInstanceUuid, String targetIp, String uploadTaskUuid,
                        BooleanSupplier canceled, Completion completion) {
        thdf.submit(new Task<Void>() {
            @Override
            public Void call() {
                try {
                    activateUploadedVddk(vmInstanceUuid, targetIp, uploadTaskUuid, canceled, completion);
                } catch (RuntimeException e) {
                    completion.fail(err(GENERIC_ERROR,
                            "failed to activate uploaded ZMigrate VDDK on VM[uuid:%s]: %s",
                            vmInstanceUuid, summarizeVddkError(e.getMessage())));
                }
                return null;
            }

            @Override
            public String getName() {
                return String.format("activate uploaded ZMigrate VDDK on VM[uuid:%s]", vmInstanceUuid);
            }
        });
    }

    private void activateUploadedVddk(String vmInstanceUuid, String targetIp, String uploadTaskUuid,
                                      BooleanSupplier canceled, Completion completion) {
        if (canceled.getAsBoolean()) {
            completion.fail(err(GENERIC_ERROR, "ZMigrate VDDK upload task[uuid:%s] was canceled",
                    uploadTaskUuid));
            return;
        }

        String activateUploadCommand = buildVddkActivationCommand(uploadTaskUuid);
        ShellResult activateUploadResult = createSshClient().runWithSudoPassword(
                targetIp, activateUploadCommand, VDDK_INSTALL_TIMEOUT_SECONDS);
        if (activateUploadResult.getRetCode() != 0) {
            completion.fail(vddkUploadCommandError("activate uploaded VDDK", activateUploadResult));
            return;
        }
        String previousUploadTaskUuid = uploadedVddkTaskUuid(vmInstanceUuid);
        createVddkUploadedTag(vmInstanceUuid, uploadTaskUuid);
        cleanupObsoleteVddkGenerations(
                targetIp, uploadTaskUuid, previousUploadTaskUuid);
        completion.success();
    }

    static String buildVddkActivationCommand(String uploadTaskUuid) {
        String uploadPath = vddkUploadPath(uploadTaskUuid);
        String generationPath = vddkGenerationPath(uploadTaskUuid);
        return String.format(
                "sudo -S -p '' sh -c 'if test -f %s && test ! -L %s; " +
                        "then rm -f -- %s; exit 0; fi; " +
                        "test -f %s && test ! -L %s && mv -T -- %s %s'",
                generationPath, generationPath, uploadPath,
                uploadPath, uploadPath, uploadPath, generationPath);
    }

    private void cleanupObsoleteVddkGenerations(String targetIp, String currentTaskUuid,
                                                String previousTaskUuid) {
        String command = buildVddkGenerationCleanupCommand(currentTaskUuid, previousTaskUuid);
        ShellResult result = createSshClient().runWithSudoPassword(
                targetIp, command, VDDK_INSTALL_TIMEOUT_SECONDS);
        if (result.getRetCode() != 0) {
            logger.warn(String.format(
                    "failed to clean obsolete ZMigrate VDDK generations on VM[ip:%s]: %s",
                    targetIp, summarizeVddkError(result.getStderr())));
        }
    }

    static String buildVddkGenerationCleanupCommand(String currentTaskUuid,
                                                    String previousTaskUuid) {
        String currentPath = vddkGenerationPath(currentTaskUuid);
        String previousClause = "";
        if (previousTaskUuid != null && !previousTaskUuid.equals(currentTaskUuid)) {
            try {
                previousClause = String.format(" ! -path '%s'", vddkGenerationPath(previousTaskUuid));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return String.format(
                "sudo -S -p '' sh -c \"find /root -maxdepth 1 -type f -name 'vddk-*.tar.gz' " +
                        "! -path '%s'%s -delete\"",
                currentPath, previousClause);
    }

    private ErrorCode vddkUploadCommandError(String command, ShellResult result) {
        return err(GENERIC_ERROR,
                "failed to execute %s for ZMigrate VDDK, return code: %s, error: %s",
                command, result.getRetCode(), summarizeVddkError(result.getStderr()));
    }

    private String summarizeVddkError(String value) {
        String details = value == null ? "" : value;
        return details.length() > 1024 ? details.substring(0, 1024) : details;
    }

    private void createVddkUploadedTag(String vmUuid, String uploadTaskUuid) {
        SystemTagCreator creator = ZMIGRATE_VDDK_UPLOADED.newSystemTagCreator(vmUuid);
        creator.inherent = false;
        creator.recreate = true;
        creator.setTagByTokens(map(e(ZMIGRATE_VDDK_UPLOAD_TASK_UUID_TOKEN, uploadTaskUuid)));
        creator.create();
    }

    private synchronized void startVddkDistributionTask() {
        if (vddkDistributionTask != null) {
            vddkDistributionTask.stop();
        }

        vddkDistributionTask = new VddkDistributionTask();
        thdf.submitCancelablePeriodicTask(vddkDistributionTask);
    }

    private class VddkDistributionTask extends CancelablePeriodicTask {
        private final AtomicBoolean stopped = new AtomicBoolean(false);

        void stop() {
            stopped.set(true);
        }

        @Override
        public boolean run() {
            if (stopped.get()) {
                return true;
            }
            triggerVddkDistributionScan();
            return stopped.get();
        }

        @Override
        public TimeUnit getTimeUnit() {
            return TimeUnit.SECONDS;
        }

        @Override
        public long getInterval() {
            return ZMigrateGlobalConfig.VDDK_DISTRIBUTION_SCAN_INTERVAL.value(Long.class);
        }

        @Override
        public String getName() {
            return "scan ZMigrate VDDK distribution";
        }
    }

    private void triggerVddkDistributionScan() {
        scanVddkDistributionOnce(new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to scan ZMigrate VDDK distribution: %s",
                        errorCode.getReadableDetails()));
            }
        });
    }

    void scanVddkDistributionOnce(Completion completion) {
        if (!vddkDistributionReady.get()) {
            completion.success();
            return;
        }
        if (!vddkDistributionScanning.compareAndSet(false, true)) {
            completion.success();
            return;
        }

        VddkDistributionSource source;
        try {
            source = findVddkDistributionSource();
        } catch (Exception e) {
            vddkDistributionScanning.set(false);
            completion.fail(e instanceof OperationFailureException
                    ? ((OperationFailureException) e).getErrorCode()
                    : err(GENERIC_ERROR, "failed to resolve ZMigrate VDDK source: %s", e.getMessage()));
            return;
        }
        if (source == null) {
            vddkDistributionScanning.set(false);
            completion.success();
            return;
        }

        List<VmInstanceVO> targets;
        try {
            targets = findVddkDistributionTargets().stream()
                    .filter(vm -> destinationMaker.isManagedByUs(vm.getUuid()))
                    .filter(vm -> !source.uploadTaskUuid.equals(installedVddkTaskUuid(vm.getUuid())))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            vddkDistributionScanning.set(false);
            completion.fail(err(GENERIC_ERROR,
                    "failed to find ZMigrate VDDK distribution targets: %s", e.getMessage()));
            return;
        }

        if (targets.isEmpty()) {
            vddkDistributionScanning.set(false);
            completion.success();
            return;
        }

        new While<>(targets).step((vm, whileCompletion) ->
                distributeVddk(source, vm.getUuid(), new Completion(whileCompletion) {
                    @Override
                    public void success() {
                        whileCompletion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to distribute ZMigrate VDDK to VM[uuid:%s]: %s",
                                vm.getUuid(), errorCode.getReadableDetails()));
                        whileCompletion.done();
                    }
                }), VDDK_DISTRIBUTION_CONCURRENCY).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                vddkDistributionScanning.set(false);
                completion.success();
            }
        });
    }

    private List<VmInstanceVO> findVddkDistributionTargets() {
        Set<String> targetVmUuids = new LinkedHashSet<>();
        targetVmUuids.addAll(findZMigrateVmUuids(ZMIGRATE_MANAGEMENT));
        targetVmUuids.addAll(findZMigrateVmUuids(ZMIGRATE_GATEWAY));
        if (targetVmUuids.isEmpty()) {
            return Collections.emptyList();
        }

        return Q.New(VmInstanceVO.class)
                .in(VmInstanceVO_.uuid, targetVmUuids)
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .list();
    }

    private VddkDistributionSource findVddkDistributionSource() {
        String managementVmUuid = findZMigrateVmUuid(ZMIGRATE_MANAGEMENT);
        if (managementVmUuid == null) {
            return null;
        }
        VmInstanceVO managementVm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, managementVmUuid)
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .find();
        if (managementVm == null) {
            return null;
        }

        String uploadTaskUuid = uploadedVddkTaskUuid(managementVmUuid);
        if (uploadTaskUuid == null) {
            return null;
        }
        return new VddkDistributionSource(managementVmUuid,
                requireVmManagementIp(managementVmUuid), uploadTaskUuid);
    }

    private void distributeVddk(VddkDistributionSource source, String vmUuid, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return "zmigrate-vddk-distribute-" + vmUuid;
            }

            @Override
            public void run(SyncTaskChain chain) {
                thdf.submit(new Task<Void>() {
                    @Override
                    public Void call() {
                        try {
                            distributeVddkNow(source, vmUuid, completion);
                        } catch (OperationFailureException e) {
                            completion.fail(e.getErrorCode());
                        } catch (Throwable t) {
                            completion.fail(err(GENERIC_ERROR,
                                    "unexpected failure while distributing VDDK to VM[uuid:%s]: %s",
                                    vmUuid, t.getMessage()));
                        } finally {
                            chain.next();
                        }
                        return null;
                    }

                    @Override
                    public String getName() {
                        return "distribute ZMigrate VDDK to VM " + vmUuid;
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void distributeVddkNow(VddkDistributionSource source, String vmUuid,
                                   Completion completion) {
        if (!Objects.equals(source.uploadTaskUuid, uploadedVddkTaskUuid(source.managementVmUuid))) {
            completion.success();
            return;
        }
        if (Objects.equals(source.uploadTaskUuid, installedVddkTaskUuid(vmUuid))) {
            completion.success();
            return;
        }

        VmInstanceVO target = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .find();
        if (target == null || !isVddkDistributionTarget(target)) {
            completion.success();
            return;
        }
        String targetIp;
        try {
            targetIp = requireVmManagementIp(vmUuid);
        } catch (CloudRuntimeException e) {
            completion.fail(err(GENERIC_ERROR,
                    "failed to get management IP for VM[uuid:%s]: %s", vmUuid, e.getMessage()));
            return;
        }

        long outerTimeout = VDDK_DISTRIBUTION_TIMEOUT_SECONDS + VDDK_INSTALL_TIMEOUT_SECONDS + 60;
        ShellResult result = createSshClient().distributeVddk(
                source.managementIp, targetIp, source.uploadTaskUuid, outerTimeout);
        if (result.getRetCode() != 0) {
            completion.fail(err(GENERIC_ERROR,
                    "ZMigrate VDDK distribution to VM[uuid:%s] failed with return code %s: %s",
                    vmUuid, result.getRetCode(), summarizeVddkError(result.getStderr())));
            return;
        }

        if (!Objects.equals(source.uploadTaskUuid, uploadedVddkTaskUuid(source.managementVmUuid))) {
            completion.success();
            return;
        }
        createVddkInstalledTag(vmUuid, source.uploadTaskUuid);
        completion.success();
    }

    private boolean isVddkDistributionTarget(VmInstanceVO vm) {
        return ZMIGRATE_MANAGEMENT.hasTag(vm.getUuid()) ||
                ZMIGRATE_GATEWAY.hasTag(vm.getUuid());
    }

    private String requireVmManagementIp(String vmUuid) {
        String ip = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, vmUuid)
                .eq(VmNicVO_.deviceId, 0)
                .select(VmNicVO_.ip)
                .findValue();
        if (!NetworkUtils.isIpv4Address(ip)) {
            throw new CloudRuntimeException(String.format(
                    "VM[uuid:%s] has no valid IPv4 management address", vmUuid));
        }
        return ip;
    }

    private String uploadedVddkTaskUuid(String gatewayVmUuid) {
        return ZMIGRATE_VDDK_UPLOADED.getTokenByResourceUuid(
                gatewayVmUuid, ZMIGRATE_VDDK_UPLOAD_TASK_UUID_TOKEN);
    }

    private String installedVddkTaskUuid(String vmUuid) {
        return ZMIGRATE_VDDK_INSTALLED.getTokenByResourceUuid(
                vmUuid, ZMIGRATE_VDDK_UPLOAD_TASK_UUID_TOKEN);
    }

    private void createVddkInstalledTag(String vmUuid, String uploadTaskUuid) {
        SystemTagCreator creator = ZMIGRATE_VDDK_INSTALLED.newSystemTagCreator(vmUuid);
        creator.inherent = false;
        creator.recreate = true;
        creator.setTagByTokens(map(e(ZMIGRATE_VDDK_UPLOAD_TASK_UUID_TOKEN, uploadTaskUuid)));
        creator.create();
    }

    private static class VddkDistributionSource {
        private final String managementVmUuid;
        private final String managementIp;
        private final String uploadTaskUuid;

        private VddkDistributionSource(String managementVmUuid, String managementIp,
                                       String uploadTaskUuid) {
            this.managementVmUuid = managementVmUuid;
            this.managementIp = managementIp;
            this.uploadTaskUuid = uploadTaskUuid;
        }
    }

    // ------------------------------------- EstimatedImageSizeExtensionPoint ------------------------------------

    @Override
    public long getEstimatedImageTotalSize() {
        return IMAGE_ESTIMATED_TOTAL_SIZE.value(Long.class);
    }

    // ------------------------------------- Utility Handers ------------------------------------

    private ZMigrateHttpClient createHttpClient() {
        return Platform.New(ZMigrateHttpClient::new);
    }

    private ZMigrateSshClient createSshClient() {
        return sshClientFactory.get();
    }
}
