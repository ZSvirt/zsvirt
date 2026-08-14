package org.zstack.storage.primary.imagestore.ceph;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.HasThreadContext;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.CreateImageExtensionPoint;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.BackupStorageMediator;
import org.zstack.header.storage.primary.MediatorDownloadParam;
import org.zstack.header.storage.primary.MediatorUploadParam;
import org.zstack.header.storage.primary.PrimaryToBackupStorageMediator;
import org.zstack.kvm.KVMConstant;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2017/11/3.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephPrimaryToImageStoreBackupStorageMediatorImpl extends BackupStorageMediator implements PrimaryToBackupStorageMediator {
    private static final CLogger logger = Utils.getLogger(CephPrimaryToImageStoreBackupStorageMediatorImpl.class);

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;

    public static class DownloadCmd extends CephPrimaryStorageBase.AgentCommand implements HasThreadContext {
        String hostname;
        String username;
        String bsInstallPath;
        String psInstallPath;
        String fsid;
        boolean shareable;
        int concurrency = ImageStoreGlobalConfig.BLOB_DOWNLOAD_CONCURRENCY.value(Integer.class);
    }

    static class DownloadRsp extends CephPrimaryStorageBase.AgentResponse {
    }

    public static class UploadCmd extends CephPrimaryStorageBase.AgentCommand implements HasThreadContext {
        String imageUuid;
        String hostname;
        String srcPath;
        String dstPath;
        String description;
        int concurrency = ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY.value(Integer.class);
    }

    public static class UploadRsp extends CephPrimaryStorageBase.AgentResponse {
        String installPath;
    }

    @Override
    public String getSupportedPrimaryStorageType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public String getSupportedBackupStorageType() {
        return ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE;
    }

    @Override
    public List<String> getSupportedHypervisorTypes() {
        return Collections.singletonList(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    private ImageStoreBackupStorageInventory getInventory() {
        return  ImageStoreBackupStorageInventory.valueOf(dbf.findByUuid(backupStorage.getUuid(), ImageStoreBackupStorageVO.class));
    }

    private List<CephPrimaryStorageMonBase> getCephMons(final CephPrimaryStorageVO vo) {
        List<CephPrimaryStorageMonBase> mons = new ArrayList<CephPrimaryStorageMonBase>();
        for (CephPrimaryStorageMonVO monvo : vo.getMons()) {
            if (monvo.getStatus() == MonStatus.Connected) {
                mons.add(new CephPrimaryStorageMonBase(monvo));
            }
        }
        if (mons.isEmpty()) {
            throw new OperationFailureException(operr(
                    "all ceph mons of primary storage[uuid:%s] are not in Connected state", vo.getUuid())
            );
        }

        Collections.shuffle(mons);
        return mons;
    }

    private DownloadCmd buildDownloadCmd(final MediatorDownloadParam param, final ImageStoreBackupStorageInventory inv, final CephPrimaryStorageVO vo) {
        DownloadCmd cmd = new DownloadCmd();
        cmd.username = inv.username;
        cmd.hostname = inv.hostname;
        cmd.psInstallPath = param.getInstallPath();
        cmd.fsid = vo.getFsid();
        cmd.bsInstallPath = param.getImage().getSelectedBackupStorage().getInstallPath();
        cmd.shareable = param.isShareable();
        ResourceConfig rc = rcf.getResourceConfig(ImageStoreGlobalConfig.BLOB_DOWNLOAD_CONCURRENCY.getIdentity());
        cmd.concurrency = rc.getResourceConfigValue(inv.getUuid(), Integer.class);
        return cmd;
    }

    private void getDownloadCredential(String bsUuid, ReturnValueCompletion<String> completion) {
        GetImageStoreBackupStorageDownloadCredentialMsg gmsg = new GetImageStoreBackupStorageDownloadCredentialMsg();
        gmsg.setBackupStorageUuid(bsUuid);
        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                final GetImageStoreBackupStorageDownloadCredentialReply r = reply.castReply();
                completion.success(r.getHostname());
            }
        });
    }

    private void downloadBits(final MediatorDownloadParam param, Completion completion) {
        DebugUtils.Assert(param.getPrimaryStorageUuid() != null, "primaryStorageUuid cannot be null in MediatorDowloadParam!");
        CephPrimaryStorageVO vo = dbf.findByUuid(param.getPrimaryStorageUuid(), CephPrimaryStorageVO.class);
        if (vo == null) {
            completion.fail(Platform.operr("CephPrimaryStorage[%s] not existed!", param.getPrimaryStorageUuid()));
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-image-%s-from-bs-%s", param.getImage().getInventory().getUuid(),
                param.getImage().getSelectedBackupStorage().getBackupStorageUuid()));
        chain.then(new ShareFlow() {
            ImageInventory image = param.getImage().getInventory();
            ImageBackupStorageRefInventory bsRef = param.getImage().getSelectedBackupStorage();
            String hostName;
            ExportImageFromBackupStorageReply eReply;

            private void setUpLegacyDownloadFlow() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DownloadCmd cmd = buildDownloadCmd(param, getInventory(), vo);
                        if (!StringUtils.isEmpty(hostName)) {
                            cmd.hostname = hostName;
                        }

                        CephPrimaryStorageBase psbase = new CephPrimaryStorageBase(vo);
                        psbase.new HttpCaller(ImageStoreBackupStorageConstant.CEPH_IMAGESTORE_DOWNLOAD_PATH, cmd, DownloadRsp.class, new ReturnValueCompletion(trigger) {
                            @Override
                            public void success(Object returnValue) {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        }).call();
                    }
                });
            }

            private void setUpFuseDownloadFlow() {
                flow(new Flow() {
                    String __name__ = "export-image-as-remote-target";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ExportImageFromBackupStorageMsg emsg = new ExportImageFromBackupStorageMsg();
                        emsg.setImageUuid(image.getUuid());
                        emsg.setTargetProtocol(RemoteTargetProtocol.NBD);
                        emsg.setBackupStorageUuid(bsRef.getBackupStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(emsg, BackupStorageConstant.SERVICE_ID, bsRef.getBackupStorageUuid());
                        bus.send(emsg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                eReply = reply.castReply();
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (eReply == null) {
                            trigger.rollback();
                            return;
                        }

                        DeleteExportedImageFromImageStoreBackupStorageMsg dmsg = new DeleteExportedImageFromImageStoreBackupStorageMsg();
                        dmsg.setRawPath(new NbdRemoteTarget(eReply.getImageUrl()).getTargetUri());
                        dmsg.setBackupStorageUuid(bsRef.getBackupStorageUuid());
                        dmsg.setImageUuid(image.getUuid());
                        dmsg.setTargetProtocol(RemoteTargetProtocol.NBD);
                        bus.makeLocalServiceId(dmsg, BackupStorageConstant.SERVICE_ID);
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-from-remote-target";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CephPrimaryStorageBase.DownloadBitsFromRemoteTargetCmd cmd = new CephPrimaryStorageBase.DownloadBitsFromRemoteTargetCmd();

                        cmd.setPrimaryStorageInstallPath(param.getInstallPath());
                        cmd.setRemoteTargetUri(new NbdRemoteTarget(eReply.getImageUrl()).getTargetUri(hostName));
                        CephPrimaryStorageBase psbase = new CephPrimaryStorageBase(vo);
                        psbase.new HttpCaller(CephPrimaryStorageBase.DOWNLOAD_BITS_FROM_REMOTE_TARGET_PATH, cmd, CephPrimaryStorageBase.DownloadBitsFromRemoteTargetRsp.class, new ReturnValueCompletion(trigger) {
                            @Override
                            public void success(Object returnValue) {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        }).call();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-nbd";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DeleteExportedImageFromImageStoreBackupStorageMsg dmsg = new DeleteExportedImageFromImageStoreBackupStorageMsg();
                        dmsg.setRawPath(new NbdRemoteTarget(eReply.getImageUrl()).getTargetUri());
                        dmsg.setBackupStorageUuid(bsRef.getBackupStorageUuid());
                        dmsg.setImageUuid(image.getUuid());
                        dmsg.setTargetProtocol(RemoteTargetProtocol.NBD);
                        bus.makeLocalServiceId(dmsg, BackupStorageConstant.SERVICE_ID);
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.next();
                            }
                        });
                    }
                });
            }

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-bs-credential";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        getDownloadCredential(bsRef.getBackupStorageUuid(), new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String returnValue) {
                                hostName = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                if (ImageStoreSystemTags.SUPPORT_FUSE.hasTag(bsRef.getBackupStorageUuid())) {
                    setUpFuseDownloadFlow();
                } else {
                    setUpLegacyDownloadFlow();
                }

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void download(ReturnValueCompletion<String> completion) {
        checkParam();

        final MediatorDownloadParam dparam = (MediatorDownloadParam) param;
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("download-bits-to-ceph-installpath-%s", dparam.getImage().getInventory().getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                downloadBits(dparam, new Completion(completion, chain) {
                    @Override
                    public void success() {
                        completion.success(dparam.getInstallPath());
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @Override
    public void upload(ReturnValueCompletion<String> completion) {
        checkParam();
        final MediatorUploadParam uparam = (MediatorUploadParam) param;
        uploadBits(uparam, completion);
    }

    private void uploadBits(final MediatorUploadParam param, ReturnValueCompletion<String> completion) {
        CephPrimaryStorageVO vo = dbf.findByUuid(param.primaryStorageUuid, CephPrimaryStorageVO.class);
        getDownloadCredential(backupStorage.getUuid(), new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String hostname) {
                param.backupStorageUuid = backupStorage.getUuid();
                UploadCmd cmd = buildUploadCmd(param);
                cmd.hostname = hostname;

                CephPrimaryStorageBase psbase = new CephPrimaryStorageBase(vo);
                psbase.new HttpCaller(ImageStoreBackupStorageConstant.CEPH_IMAGESTORE_COMMIT_PATH, cmd, UploadRsp.class, new ReturnValueCompletion<UploadRsp>(completion) {
                    @Override
                    public void success(UploadRsp rsp) {
                        completion.success(rsp.installPath);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                }).call();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private UploadCmd buildUploadCmd(final MediatorUploadParam param) {
        UploadCmd cmd = new UploadCmd();
        StringBuilder desc = new StringBuilder();
        for (CreateImageExtensionPoint ext : pluginRgty.getExtensionList(CreateImageExtensionPoint.class)) {
            String tmp = ext.getImageDescription(param.image);
            if (tmp != null && !tmp.trim().equals("")) {
                desc.append(tmp);
            }
        }
        cmd.description = desc.toString();
        cmd.srcPath = param.primaryStorageInstallPath;
        cmd.dstPath = param.backupStorageInstallPath;
        cmd.imageUuid = param.image.getUuid();
        cmd.concurrency = rcf.getResourceConfigValue(ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY, param.backupStorageUuid, Integer.class);

        return cmd;
    }

    @Override
    public boolean deleteWhenRollbackDownload() {
        return false;
    }
}
