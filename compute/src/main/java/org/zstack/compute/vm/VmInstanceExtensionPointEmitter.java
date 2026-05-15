package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.extension.VmInstancePreStartExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;

public class VmInstanceExtensionPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(VmInstanceExtensionPointEmitter.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ErrorFacade errf;

    private List<VmInstanceBeforeStartExtensionPoint> VmInstanceBeforeStartExtensions;
    private List<VmInstanceResumeExtensionPoint> VmInstanceResumeExtensionPoints;
    private List<VmInstanceStartNewCreatedVmExtensionPoint> startNewCreatedVmExtensions;
    private List<BeforeVmInstanceStopExtensionPoint> beforeVmStopExtensions;
    private List<VmInstanceStopExtensionPoint> stopVmExtensions;
    private List<VmInstanceRebootExtensionPoint> rebootVmExtensions;
    private List<VmInstanceDestroyExtensionPoint> destroyVmExtensions;
    private List<VmInstanceStartExtensionPoint> startVmExtensions;
    private List<VmInstancePreStartExtensionPoint> preStartVmExtensions;
    private List<VmInstanceMigrateExtensionPoint> migrateVmExtensions;
    private List<VmAttachVolumeExtensionPoint> attachVolumeExtensions;
    private List<VmDetachVolumeExtensionPoint> detachVolumeExtensions;
    private List<VmCapabilitiesExtensionPoint> capabilitiesExtensionPoints;
    private List<CleanUpAfterVmFailedToStartExtensionPoint> cleanUpAfterVmFailedToStartExtensionPoints;
    private List<CleanUpAfterVmChangeImageExtensionPoint> cleanUpAfterVmChangeImageExtensionPoints;
    private List<VmNicChangeStateExtensionPoint> vmNicChangeStateExtensionPoints;
    private List<SshKeyPairAssociateExtensionPoint> sshKeyPairAssociateExtensionPoints;

    public List<ErrorCode> handleSystemTag(String vmUuid, List<String> tags){
        List<ErrorCode> errorCodes = new ArrayList<>();
        CollectionUtils.safeForEach(VmInstanceBeforeStartExtensions, extension -> {
            ErrorCode errorCode = extension.handleSystemTag(vmUuid, tags);
            if (errorCode != null) {
                errorCodes.add(errorCode);
            }
        });
        return errorCodes;
    }

    public ErrorCode preStartNewCreatedVm(VmInstanceInventory inv) {
        for (VmInstanceStartNewCreatedVmExtensionPoint ext : startNewCreatedVmExtensions) {
            try {
                String err = ext.preStartNewCreatedVm(inv);
                if (err != null) {
                    return operr("VmInstanceStartNewCreatedVmExtensionPoint[%s] refuses to create vm[uuid:%s]",
                            ext.getClass().getSimpleName(), inv.getUuid())
                            .withException(err);
                }
            } catch (Exception e) {
                logger.warn(String.format("Unhandled exception while calling %s", ext.getClass().getName()), e);
            }
        }
        return null;
    }

    public void beforeStartNewCreatedVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(startNewCreatedVmExtensions, arg -> arg.beforeStartNewCreatedVm(inv));
    }

    public void afterStartNewCreatedVm(final VmInstanceInventory inv) {
        CollectionUtils.forEach(startNewCreatedVmExtensions, arg -> arg.afterStartNewCreatedVm(inv));
    }

    public void failedToStartNewCreatedVm(final VmInstanceInventory inv, final ErrorCode reason) {
        CollectionUtils.forEach(startNewCreatedVmExtensions, arg -> arg.failedToStartNewCreatedVm(inv, reason));
    }

    public void beforeVmStop(VmInstanceInventory inv, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.allowEmptyFlow();
        for(BeforeVmInstanceStopExtensionPoint ext: beforeVmStopExtensions) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    ext.beforeVmInstanceStop(inv, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    public ErrorCode preStopVm(VmInstanceInventory inv) {
        for (VmInstanceStopExtensionPoint ext : stopVmExtensions) {
            try {
                String err = ext.preStopVm(inv);
                if (err != null) {
                    return operr("VmInstanceStopVmExtensionPoint[%s] refuses to stop vm[uuid:%s]",
                            ext.getClass().getSimpleName(), inv.getUuid())
                            .withException(err);
                }
            } catch (Exception e) {
                logger.warn(String.format("Unhandled exception while calling %s", ext.getClass().getSimpleName()), e);
            }
        }
        return null;
    }

    public void beforeStopVm(final VmInstanceInventory inv) {
        CollectionUtils.forEach(stopVmExtensions, arg -> arg.beforeStopVm(inv));
    }

    public void afterStopVm(final VmInstanceInventory inv) {
        CollectionUtils.forEach(stopVmExtensions, arg -> arg.afterStopVm(inv));
    }

    public void failedToStopVm(final VmInstanceInventory inv, final ErrorCode reason) {
        CollectionUtils.forEach(stopVmExtensions, arg -> arg.failedToStopVm(inv, reason));
    }

    public ErrorCode preRebootVm(final VmInstanceInventory inv) {
        for (VmInstanceRebootExtensionPoint ext : rebootVmExtensions) {
            try {
                String err = ext.preRebootVm(inv);
                if (err != null) {
                    return operr("VmInstanceRebootExtensionPoint[%s] refuses to reboot vm[uuid:%s]",
                            ext.getClass().getSimpleName(), inv.getUuid())
                            .withException(err);
                }
            } catch (Exception e) {
                logger.warn(String.format("Unhandled exception while calling %s", ext.getClass().getSimpleName()), e);
            }
        }
        return null;
    }

    public void beforeRebootVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(rebootVmExtensions, arg -> arg.beforeRebootVm(inv));
    }

    public void afterRebootVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(rebootVmExtensions, arg -> arg.afterRebootVm(inv));
    }

    public void failedToRebootVm(final VmInstanceInventory inv, final ErrorCode reason) {
        CollectionUtils.safeForEach(rebootVmExtensions, arg -> arg.failedToRebootVm(inv, reason));
    }

    public ErrorCode preDestroyVm(VmInstanceInventory inv) {
        for (VmInstanceDestroyExtensionPoint ext : destroyVmExtensions) {
            try {
                String err = ext.preDestroyVm(inv);
                if (err != null) {
                    return operr("VmInstanceDestroyVmExtensionPoint[%s] refuses to destroy vm[uuid:%s]",
                            ext.getClass().getSimpleName(), inv.getUuid())
                            .withException(err);
                }
            } catch (Exception e) {
                logger.warn(String.format("Unhandled exception while calling %s", ext.getClass().getSimpleName()), e);
            }
        }
        return null;
    }

    public void beforeDestroyVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(destroyVmExtensions, arg -> arg.beforeDestroyVm(inv));
    }

    public void afterDestroyVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(destroyVmExtensions, arg -> arg.afterDestroyVm(inv));
    }

    public void failedToDestroyVm(final VmInstanceInventory inv, final ErrorCode reason) {
        CollectionUtils.safeForEach(destroyVmExtensions, arg -> arg.failedToDestroyVm(inv, reason));
    }

    public void afterResumeVm(VmInstanceInventory inv) {
        CollectionUtils.safeForEach(VmInstanceResumeExtensionPoints, arg -> arg.afterResumeVm(inv));
    }

    public ErrorCode preStartVm(VmInstanceInventory inv) {
        ErrorCodeList errList = new ErrorCodeList();
        for (VmInstancePreStartExtensionPoint ext : preStartVmExtensions) {
            try {
                ErrorCode err = ext.preStartVm(inv);
                if (err != null) errList.add(err);
            } catch (Exception e) {
                errList.add(operr("preStartVm failed in %s", ext.getClass().getSimpleName())
                        .withException(e));
            }
        }
        int n = errList.size();
        if (n == 0) {
            return null;
        }
        if (n == 1) {
            return errList.getCauses().get(0);
        }
        return multiErr(errList);
    }

    public void beforeStartVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(startVmExtensions, arg -> arg.beforeStartVm(inv));
    }

    public void afterStartVm(final VmInstanceInventory inv) {
        CollectionUtils.safeForEach(startVmExtensions, arg -> arg.afterStartVm(inv));
    }

    public void failedToStartVm(final VmInstanceInventory inv, final ErrorCode reason) {
        CollectionUtils.safeForEach(startVmExtensions, arg -> arg.failedToStartVm(inv, reason));
    }

    public void preMigrateVm(final VmInstanceInventory inv, final String dstHostUuid, Completion completion) {
        new While<>(migrateVmExtensions).each((ext, comp) -> ext.preMigrateVm(inv, dstHostUuid, new Completion(comp) {
            @Override
            public void success() {
                comp.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.addError(errorCode);
                comp.allDone();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().size() > 0) {
                    completion.fail(errorCodeList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    public void beforeMigrateVm(final VmInstanceInventory inv, final String dstHostUuid) {
        CollectionUtils.safeForEach(migrateVmExtensions, arg -> arg.beforeMigrateVm(inv, dstHostUuid));
    }

    public void postMigrateVm(final VmInstanceInventory inv, final String dstHostUuid, Completion completion) {
        new While<>(migrateVmExtensions).each((ext, comp) -> ext.postMigrateVm(inv, dstHostUuid, new Completion(comp) {
            @Override
            public void success() {
                comp.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.addError(errorCode);
                comp.allDone();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    public void afterMigrateVm(final VmInstanceInventory inv, final String srcHostUuid, NoErrorCompletion completion) {
        new While<>(migrateVmExtensions).each((ext, comp) -> ext.afterMigrateVm(inv, srcHostUuid, new NoErrorCompletion(comp) {
            @Override
            public void done() {
                comp.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    public void failedToMigrateVm(final VmInstanceInventory inv, final String dstHostUuid, final ErrorCode reason, NoErrorCompletion completion) {
        new While<>(migrateVmExtensions).each((ext, comp) -> ext.failedToMigrateVm(inv, dstHostUuid, reason, new NoErrorCompletion(comp) {
            @Override
            public void done() {
                comp.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        new While<>(attachVolumeExtensions).each((ext, comp) -> ext.preAttachVolume(vm, volume, new Completion(comp) {
            @Override
            public void success() {
                comp.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.addError(errorCode);
                comp.allDone();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().size() > 0) {
                    completion.fail(errorCodeList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    public void beforeAttachVolume(final VmInstanceInventory vm, final VolumeInventory volume, Map data) {
        CollectionUtils.safeForEach(attachVolumeExtensions, arg -> arg.beforeAttachVolume(vm, volume, data));
    }

    public void afterAttachVolume(final VmInstanceInventory vm, final VolumeInventory volume) {
        CollectionUtils.safeForEach(attachVolumeExtensions, arg -> arg.afterAttachVolume(vm, volume));
    }

    public void failedToAttachVolume(final VmInstanceInventory vm, final VolumeInventory volume, final ErrorCode errorCode, Map data) {
        CollectionUtils.safeForEach(attachVolumeExtensions, arg -> arg.failedToAttachVolume(vm, volume, errorCode, data));
    }

    public void afterInstantiateVolume(VmInstanceInventory inv, VolumeInventory volume, NoErrorCompletion completion) {
        new While<>(attachVolumeExtensions).each((ext, comp) -> ext.afterInstantiateVolume(inv, volume, new Completion(comp) {
            @Override
            public void success() {
                comp.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    public void preDetachVolume(final VmInstanceInventory vm, final VolumeInventory volume, Completion completion) {
        new While<>(detachVolumeExtensions).each((ext, comp) -> ext.preDetachVolume(vm, volume, new Completion(comp) {
            @Override
            public void success() {
                comp.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                comp.addError(errorCode);
                comp.allDone();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().size() > 0) {
                    completion.fail(errorCodeList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    public void beforeDetachVolume(final VmInstanceInventory vm, final VolumeInventory volume) {
        CollectionUtils.safeForEach(detachVolumeExtensions, arg -> arg.beforeDetachVolume(vm, volume));
    }

    public void afterDetachVolume(final VmInstanceInventory vm, final VolumeInventory volume, final Completion completion) {
        if (detachVolumeExtensions.isEmpty()) {
            completion.success();
            return;
        }
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        CollectionUtils.safeForEach(detachVolumeExtensions, arg -> chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                arg.afterDetachVolume(vm, volume, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("found a error when calling afterDetachVolume[volumeUuid:%s] extension point" +
                                ":%s, ignore it", volume.getUuid(), errorCode.getDetails()));
                        trigger.next();
                    }
                });
            }
        }));

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    public void failedToDetachVolume(final VmInstanceInventory vm, final VolumeInventory volume, final ErrorCode errorCode) {
        CollectionUtils.safeForEach(detachVolumeExtensions, arg -> arg.failedToDetachVolume(vm, volume, errorCode));
    }

    public void getVmCapabilities(final VmInstanceInventory vm, final VmCapabilities capabilities) {
        CollectionUtils.safeForEach(capabilitiesExtensionPoints, arg -> arg.checkVmCapability(vm, capabilities));
    }

    public void cleanUpAfterVmFailedToStart(final VmInstanceInventory vm, VmOperation op) {
        CollectionUtils.safeForEach(cleanUpAfterVmFailedToStartExtensionPoints, arg -> arg.cleanUpAfterVmFailedToStart(vm, op));
    }

    public void cleanUpAfterVmChangeImage(final VmInstanceInventory vm) {
        CollectionUtils.safeForEach(cleanUpAfterVmChangeImageExtensionPoints, arg -> arg.cleanUpAfterVmChangeImage(vm));
    }

    public void afterChangeVmNicState(final String vmNic, final String state) {
        CollectionUtils.safeForEach(vmNicChangeStateExtensionPoints, arg -> arg.afterChangeVmNicState(vmNic, state));
    }

    public List<ErrorCode> associateSshKeyPair(String vmUuid, List<String> sshKeyUuids) {
        List<ErrorCode> errorCodes = new ArrayList<>();
        CollectionUtils.safeForEach(sshKeyPairAssociateExtensionPoints, extension -> {
            ErrorCode errorCode = extension.associateSshKeyPair(vmUuid, sshKeyUuids);
            if (errorCode != null) {
                errorCodes.add(errorCode);
            }
        });
        return errorCodes;
    }

    public List<String> fetchAssociatedSshKeyPairs(String vmUuid) {
        List<String> sshKeyPairs = new ArrayList<>();
        CollectionUtils.safeForEach(sshKeyPairAssociateExtensionPoints, extension -> {
            List<String> keyPairs = extension.fetchAssociatedSshKeyPairs(vmUuid);
            if (!keyPairs.isEmpty()) {
                sshKeyPairs.addAll(keyPairs);
            }
        });
        return sshKeyPairs;
    }

    public void cloneSshKeyPairsToVm(String originVmUuid, String destVmUuid) {
        CollectionUtils.safeForEach(sshKeyPairAssociateExtensionPoints, extension -> {
            extension.cloneSshKeyPairsToVm(originVmUuid, destVmUuid);
        });
    }

    private void populateExtensions() {
        VmInstanceBeforeStartExtensions = pluginRgty.getExtensionList(VmInstanceBeforeStartExtensionPoint.class);
        VmInstanceResumeExtensionPoints = pluginRgty.getExtensionList(VmInstanceResumeExtensionPoint.class);
        startNewCreatedVmExtensions = pluginRgty.getExtensionList(VmInstanceStartNewCreatedVmExtensionPoint.class);
        beforeVmStopExtensions = pluginRgty.getExtensionList(BeforeVmInstanceStopExtensionPoint.class);
        stopVmExtensions = pluginRgty.getExtensionList(VmInstanceStopExtensionPoint.class);
        rebootVmExtensions = pluginRgty.getExtensionList(VmInstanceRebootExtensionPoint.class);
        destroyVmExtensions = pluginRgty.getExtensionList(VmInstanceDestroyExtensionPoint.class);
        startVmExtensions = pluginRgty.getExtensionList(VmInstanceStartExtensionPoint.class);
        preStartVmExtensions = pluginRgty.getExtensionList(VmInstancePreStartExtensionPoint.class);
        migrateVmExtensions = pluginRgty.getExtensionList(VmInstanceMigrateExtensionPoint.class);
        attachVolumeExtensions = pluginRgty.getExtensionList(VmAttachVolumeExtensionPoint.class);
        detachVolumeExtensions = pluginRgty.getExtensionList(VmDetachVolumeExtensionPoint.class);
        capabilitiesExtensionPoints = pluginRgty.getExtensionList(VmCapabilitiesExtensionPoint.class);
        cleanUpAfterVmFailedToStartExtensionPoints = pluginRgty.getExtensionList(CleanUpAfterVmFailedToStartExtensionPoint.class);
        cleanUpAfterVmChangeImageExtensionPoints = pluginRgty.getExtensionList(CleanUpAfterVmChangeImageExtensionPoint.class);
        vmNicChangeStateExtensionPoints = pluginRgty.getExtensionList(VmNicChangeStateExtensionPoint.class);
        sshKeyPairAssociateExtensionPoints = pluginRgty.getExtensionList(SshKeyPairAssociateExtensionPoint.class);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
