package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.storage.primary.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.List;

class PrimaryStorageExtensionPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageExtensionPointEmitter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private List<PrimaryStorageAttachExtensionPoint> attachExts;
    private List<PrimaryStorageDetachExtensionPoint> detachExts;
    private List<PrimaryStorageDeleteExtensionPoint> delExts;
    private List<PrimaryStorageChangeStateExtensionPoint> changeExts;

    void preDelete(PrimaryStorageInventory inv) throws PrimaryStorageException {
        for (PrimaryStorageDeleteExtensionPoint extp : delExts) {
            try {
                extp.preDeletePrimaryStorage(inv);
            } catch (PrimaryStorageException pe) {
                logger.debug(String.format("%s refused to delete primary storage[uuid:%s] because %s",
                        extp.getClass().getName(), inv.getUuid(), pe.getMessage()));
                throw pe;
            } catch (Exception e) {
                logger.warn("Exception happened when calling preDelete of PrimaryStorageDeleteExtensionPoint", e);
            }
        }
    }

    void beforeDelete(final PrimaryStorageInventory inv) {
        CollectionUtils.safeForEach(delExts, new ForEachFunction<PrimaryStorageDeleteExtensionPoint>() {
            @Override
            public void run(PrimaryStorageDeleteExtensionPoint arg) {
                arg.beforeDeletePrimaryStorage(inv);
            }
        });
    }


    void afterDelete(final PrimaryStorageInventory inv) {
        CollectionUtils.safeForEach(delExts, new ForEachFunction<PrimaryStorageDeleteExtensionPoint>() {
            @Override
            public void run(PrimaryStorageDeleteExtensionPoint arg) {
                arg.afterDeletePrimaryStorage(inv);
            }
        });
    }

    void preChange(PrimaryStorageVO vo, PrimaryStorageStateEvent evt) throws PrimaryStorageException {
        PrimaryStorageState next = AbstractPrimaryStorage.getNextState(vo.getState(), evt);
        PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(vo);
        for (PrimaryStorageChangeStateExtensionPoint extp : changeExts) {
            try {
                extp.preChangePrimaryStorageState(inv, evt, next);
            } catch (PrimaryStorageException pe) {
                logger.debug(String.format("%s refused to change primary storage[uuid:%s] state from %s to %s because %s",
                        extp.getClass().getCanonicalName(), vo.getUuid(), vo.getState(), next, pe.getMessage()), pe);
                throw pe;
            } catch (Exception e) {
                logger.warn("Exception happened when calling preChangePrimaryStorageState of PrimaryStorageChangeStateExtensionPoint", e);
            }
        }
    }

    void beforeChange(PrimaryStorageVO vo, final PrimaryStorageStateEvent evt) {
        final PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(vo);
        final PrimaryStorageState next = AbstractPrimaryStorage.getNextState(vo.getState(), evt);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<PrimaryStorageChangeStateExtensionPoint>() {
            @Override
            public void run(PrimaryStorageChangeStateExtensionPoint arg) {
                arg.beforeChangePrimaryStorageState(inv, evt, next);
            }
        });
    }


    void afterChange(PrimaryStorageVO vo, final PrimaryStorageStateEvent evt, final PrimaryStorageState preState) {
        final PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<PrimaryStorageChangeStateExtensionPoint>() {
            @Override
            public void run(PrimaryStorageChangeStateExtensionPoint arg) {
                arg.afterChangePrimaryStorageState(inv, evt, preState);
            }
        });
    }

    void preAttach(PrimaryStorageVO vo, String clusterUuid) throws PrimaryStorageException {
        PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(vo);
        for (PrimaryStorageAttachExtensionPoint extp : attachExts) {
            try {
                extp.preAttachPrimaryStorage(inv, clusterUuid);
            } catch (PrimaryStorageException pe) {
                logger.debug(String.format("%s refused to attach primary storage[uuid:%s] because %s",
                        extp.getClass().getName(), inv.getUuid(), pe.getMessage()));
                throw pe;
            } catch (Exception e) {
                logger.warn("Exception happened when calling preAttach of PrimaryStorageAttachExtensionPoint", e);
            }
        }
    }

    void beforeAttach(PrimaryStorageVO vo, final String clusterUuid) {
        final PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, new ForEachFunction<PrimaryStorageAttachExtensionPoint>() {
            @Override
            public void run(PrimaryStorageAttachExtensionPoint arg) {
                arg.beforeAttachPrimaryStorage(inv, clusterUuid);
            }
        });
    }

    void failToAttach(PrimaryStorageVO vo, final String clusterUuid) {
        final PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, new ForEachFunction<PrimaryStorageAttachExtensionPoint>() {
            @Override
            public void run(PrimaryStorageAttachExtensionPoint arg) {
                arg.failToAttachPrimaryStorage(inv, clusterUuid);
            }
        });
    }


    void afterAttach(PrimaryStorageVO vo, final String clusterUuid) {
        final PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, new ForEachFunction<PrimaryStorageAttachExtensionPoint>() {
            @Override
            public void run(PrimaryStorageAttachExtensionPoint arg) {
                arg.afterAttachPrimaryStorage(inv, clusterUuid);
            }
        });
    }

    String preDetach(PrimaryStorageVO vo, String clusterUuid) throws PrimaryStorageException {
        PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(vo);
        for (PrimaryStorageDetachExtensionPoint extp : detachExts) {
            try {
                extp.preDetachPrimaryStorage(inv, clusterUuid);
            } catch (PrimaryStorageException pe) {
                logger.debug(String.format("%s refused to detach primary storage[uuid:%s, name: %s] because %s",
                        extp.getClass().getName(), inv.getUuid(), inv.getName(), pe.getMessage()));
                throw pe;
            } catch (Exception e) {
                logger.warn("Exception happened when calling preDetach of PrimaryStorageDetachExtensionPoint", e);
            }
        }

        return null;
    }

    void beforeDetach(PrimaryStorageVO vo, final String clusterUuid) {
        final PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, new ForEachFunction<PrimaryStorageDetachExtensionPoint>() {
            @Override
            public void run(PrimaryStorageDetachExtensionPoint arg) {
                arg.beforeDetachPrimaryStorage(inv, clusterUuid);
            }
        });
    }

    void failToDetach(PrimaryStorageVO vo, final String clusterUuid) {
        final PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, new ForEachFunction<PrimaryStorageDetachExtensionPoint>() {
            @Override
            public void run(PrimaryStorageDetachExtensionPoint arg) {
                arg.failToDetachPrimaryStorage(inv, clusterUuid);
            }
        });
    }

    void afterDetach(PrimaryStorageVO vo, final String clusterUuid) {
        final PrimaryStorageInventory inv = PrimaryStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, new ForEachFunction<PrimaryStorageDetachExtensionPoint>() {
            @Override
            public void run(PrimaryStorageDetachExtensionPoint arg) {
                arg.afterDetachPrimaryStorage(inv, clusterUuid);
            }
        });
    }

    @Override
    public boolean start() {
        populateExtension();
        return true;
    }

    private void populateExtension() {
        attachExts = pluginRgty.getExtensionList(PrimaryStorageAttachExtensionPoint.class);
        detachExts = pluginRgty.getExtensionList(PrimaryStorageDetachExtensionPoint.class);
        delExts = pluginRgty.getExtensionList(PrimaryStorageDeleteExtensionPoint.class);
        changeExts = pluginRgty.getExtensionList(PrimaryStorageChangeStateExtensionPoint.class);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
