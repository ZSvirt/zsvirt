package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.List;

class BackupStorageExtensionPointEmitter implements Component {
	private static final CLogger logger = Utils.getLogger(BackupStorageExtensionPointEmitter.class);

	@Autowired
	private PluginRegistry pluginRgty;

    private List<BackupStorageAttachExtensionPoint> attachExts;
    private List<BackupStorageDetachExtensionPoint> detachExts;
    private List<BackupStorageDeleteExtensionPoint> delExts;
    private List<BackupStorageChangeStateExtensionPoint> changeExts;

	void preDelete(BackupStorageInventory inv) throws BackupStorageException {
		for (BackupStorageDeleteExtensionPoint extp : delExts) {
			try {
				extp.preDeleteSecondaryStorage(inv);
			} catch (BackupStorageException be) {
                logger.debug(String.format("%s refused to delete backup storage[uuid:%s] because %s", extp.getClass().getName(), inv.getUuid(), be.getMessage()), be);
                throw be;
            } catch (Exception e) {
				logger.warn("Exception happened when calling preDelete of BackupStorageDeleteExtensionPoint", e);
			}
		}
	}

	void beforeDelete(final BackupStorageInventory inv) {
        CollectionUtils.safeForEach(delExts, new ForEachFunction<BackupStorageDeleteExtensionPoint>() {
            @Override
            public void run(BackupStorageDeleteExtensionPoint arg) {
                arg.beforeDeleteSecondaryStorage(inv);
            }
        });
	}

	
	void afterDelete(final BackupStorageInventory inv) {
        CollectionUtils.safeForEach(delExts, new ForEachFunction<BackupStorageDeleteExtensionPoint>() {
            @Override
            public void run(BackupStorageDeleteExtensionPoint arg) {
                arg.afterDeleteSecondaryStorage(inv);
            }
        });
	}
	
	void preChange(BackupStorageVO vo, BackupStorageStateEvent evt) throws BackupStorageException {
		BackupStorageState next = AbstractBackupStorage.getNextState(vo.getState(), evt);
		BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
		for (BackupStorageChangeStateExtensionPoint extp : changeExts) {
			try {
				extp.preChangeSecondaryStorageState(inv, evt, next);
			} catch(BackupStorageException be) {
                logger.debug(String.format("%s refused to change backup storage[uuid:%s] state from %s to %s because %s", extp.getClass().getCanonicalName(), vo.getUuid(), vo.getState(), next, be.getMessage()));
                throw be;
            } catch (Exception e) {
				logger.warn("Exception happened when calling preChangeBackupStorageState of BackupStorageChangeStateExtensionPoint", e);
			}
		}
	}
	
	void beforeChange(BackupStorageVO vo, final BackupStorageStateEvent evt) {
		final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
		final BackupStorageState next = AbstractBackupStorage.getNextState(vo.getState(), evt);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<BackupStorageChangeStateExtensionPoint>() {
            @Override
            public void run(BackupStorageChangeStateExtensionPoint arg) {
                arg.beforeChangeSecondaryStorageState(inv, evt, next);
            }
        });
	}

	
	void afterChange(BackupStorageVO vo, final BackupStorageStateEvent evt, final BackupStorageState preState) {
		final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<BackupStorageChangeStateExtensionPoint>() {
            @Override
            public void run(BackupStorageChangeStateExtensionPoint arg) {
                arg.afterChangeSecondaryStorageState(inv, evt, preState);
            }
        });
	}
	
	String preAttach(BackupStorageVO vo, String zoneUuid) {
		BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
		for (BackupStorageAttachExtensionPoint extp : attachExts) {
			try {
				String reason = extp.preAttachBackupStorage(inv, zoneUuid);
				if (reason != null) {
					logger.debug(String.format("%s refused to attach backup storage[uuid:%s] because %s", extp.getClass().getName(), inv.getUuid(), reason));
					return reason;
				}
			} catch (Exception e) {
				logger.warn("Exception happened when calling preAttach of BackupStorageAttachExtensionPoint", e);
			}
		}

		return null;
	}
	
	void beforeAttach(BackupStorageVO vo, final String zoneUuid) {
		final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, new ForEachFunction<BackupStorageAttachExtensionPoint>() {
            @Override
            public void run(BackupStorageAttachExtensionPoint arg) {
                arg.beforeAttachBackupStorage(inv, zoneUuid);
            }
        });
	}
	
	void failToAttach(BackupStorageVO vo, final String zoneUuid) {
		final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, new ForEachFunction<BackupStorageAttachExtensionPoint>() {
            @Override
            public void run(BackupStorageAttachExtensionPoint arg) {
                arg.failToAttachBackupStorage(inv, zoneUuid);
            }
        });
	}
	
	
	void afterAttach(BackupStorageVO vo, final String zoneUuid) {
		final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, new ForEachFunction<BackupStorageAttachExtensionPoint>() {
            @Override
            public void run(BackupStorageAttachExtensionPoint arg) {
                arg.afterAttachBackupStorage(inv, zoneUuid);
            }
        });
	}
	
	void preDetach(BackupStorageVO vo, String zoneUuid) throws BackupStorageException {
		BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
		for (BackupStorageDetachExtensionPoint extp : detachExts) {
			try {
				extp.preDetachBackupStorage(inv, zoneUuid);
            } catch (BackupStorageException be) {
                logger.debug(String.format("%s refused to detach backup storage[uuid:%s] because %s", extp.getClass().getName(), inv.getUuid(), be.getMessage()));
                throw be;
			} catch (Exception e) {
				logger.warn("Exception happened when calling preDetach of BackupStorageDetachExtensionPoint", e);
			}
		}
	}
	
	void beforeDetach(BackupStorageVO vo, final String zoneUuid) {
		final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, new ForEachFunction<BackupStorageDetachExtensionPoint>() {
            @Override
            public void run(BackupStorageDetachExtensionPoint arg) {
                arg.beforeDetachBackupStorage(inv, zoneUuid);
            }
        });
	}
	
	void failToDetach(BackupStorageVO vo, final String zoneUuid) {
		final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, new ForEachFunction<BackupStorageDetachExtensionPoint>() {
            @Override
            public void run(BackupStorageDetachExtensionPoint arg) {
                arg.afterDetachBackupStorage(inv, zoneUuid);
            }
        });
	}
	
	void afterDetach(BackupStorageVO vo, final String zoneUuid) {
		final BackupStorageInventory inv = BackupStorageInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, new ForEachFunction<BackupStorageDetachExtensionPoint>() {
            @Override
            public void run(BackupStorageDetachExtensionPoint arg) {
                arg.afterDetachBackupStorage(inv, zoneUuid);
            }
        });
	}

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        attachExts = pluginRgty.getExtensionList(BackupStorageAttachExtensionPoint.class);
        detachExts = pluginRgty.getExtensionList(BackupStorageDetachExtensionPoint.class);
        delExts = pluginRgty.getExtensionList(BackupStorageDeleteExtensionPoint.class);
        changeExts = pluginRgty.getExtensionList(BackupStorageChangeStateExtensionPoint.class);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
