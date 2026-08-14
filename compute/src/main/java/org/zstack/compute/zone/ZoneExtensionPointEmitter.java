package org.zstack.compute.zone;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.zone.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.List;

class ZoneExtensionPointEmitter implements Component {
	private static final CLogger logger = Utils.getLogger(ZoneExtensionPointEmitter.class);

	@Autowired
	private PluginRegistry pluginRgty;

    private List<ZoneDeleteExtensionPoint> delExts;
    private List<ZoneChangeStateExtensionPoint> changeExts;

	void preDelete(ZoneInventory zinv) throws ZoneException {
		for (ZoneDeleteExtensionPoint extp : delExts) {
			try {
				extp.preDeleteZone(zinv);
            } catch (ZoneException ze) {
                logger.debug(String.format("extension[%s] refused to delete zone[name: %s, uuid:%s] because %s", extp.getClass().getName(), zinv.getName(), zinv.getUuid(), ze.getMessage()));
                throw ze;
			} catch (Exception e) {
				logger.warn("Exception happened while calling " + extp.getClass().getCanonicalName() + ".preDelete, " + "zone name: " + zinv.getName()
				        + " uuid: " + zinv.getUuid(), e);
			}
		}
	}
	
	void beforeDelete(final ZoneInventory zinv) {
        CollectionUtils.safeForEach(delExts, new ForEachFunction<ZoneDeleteExtensionPoint>() {
            @Override
            public void run(ZoneDeleteExtensionPoint arg) {
                arg.beforeDeleteZone(zinv);
            }
        });
	}
	
	void afterDelete(final ZoneInventory zinv) {
        CollectionUtils.safeForEach(delExts, new ForEachFunction<ZoneDeleteExtensionPoint>() {
            @Override
            public void run(ZoneDeleteExtensionPoint arg) {
                arg.afterDeleteZone(zinv);
            }
        });
	}
	
	void preChange(ZoneVO vo, ZoneStateEvent event) throws ZoneException {
		ZoneInventory zinv = ZoneInventory.valueOf(vo);
		ZoneState next = AbstractZone.getNextState(vo.getState(), event);
		for (ZoneChangeStateExtensionPoint extp : changeExts) {
			try {
				extp.preChangeZoneState(zinv, event, next);
            } catch (ZoneException ze) {
                logger.debug(String.format("Extension: %s refused zone change state operation[ZoneStateEvent:%s] because %s", extp.getClass()
                        .getCanonicalName(), event, ze.getMessage()));
                throw ze;
			} catch (Exception e) {
				logger.warn("Exception happened while calling " + extp.getClass().getCanonicalName() + ".preChangeZoneState(), " + "zone name: " + zinv.getName()
				        + " uuid: " + zinv.getUuid(), e);
			}
		}
	}
	
	void beforeChange(ZoneVO vo, final ZoneStateEvent event) {
		final ZoneInventory zinv = ZoneInventory.valueOf(vo);
		final ZoneState next = AbstractZone.getNextState(vo.getState(), event);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<ZoneChangeStateExtensionPoint>() {
            @Override
            public void run(ZoneChangeStateExtensionPoint arg) {
                arg.beforeChangeZoneState(zinv, event, next);
            }
        });
	}
	
	void afterChange(ZoneVO vo, final ZoneStateEvent event, final ZoneState previousState) {
		final ZoneInventory zinv = ZoneInventory.valueOf(vo);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<ZoneChangeStateExtensionPoint>() {
            @Override
            public void run(ZoneChangeStateExtensionPoint arg) {
                arg.afterChangeZoneState(zinv, event, previousState);
            }
        });
	}

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        delExts = pluginRgty.getExtensionList(ZoneDeleteExtensionPoint.class);
        changeExts = pluginRgty.getExtensionList(ZoneChangeStateExtensionPoint.class);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
