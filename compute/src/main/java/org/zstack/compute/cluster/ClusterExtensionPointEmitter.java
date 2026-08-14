package org.zstack.compute.cluster;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.cluster.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.operr;

class ClusterExtensionPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(ClusterExtensionPointEmitter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private List<ClusterDeleteExtensionPoint> deleteExts;
    private List<ClusterChangeStateExtensionPoint> changeExts;
    private List<ClusterUpdateOSExtensionPoint> updateOSExts;

    void preDelete(ClusterInventory cinv) throws ClusterException {
        for (ClusterDeleteExtensionPoint extp : deleteExts) {
            try {
                extp.preDeleteCluster(cinv);
            } catch (ClusterException ce) {
                logger.warn(String.format("extension[%s] refused to delete cluster[name: %s, uuid: %s], %s", extp.getClass().getName(), cinv.getName(), cinv.getUuid(), ce.getMessage()), ce);
                throw ce;
            } catch (Exception e) {
                logger.warn("Exception happened while calling " + extp.getClass().getCanonicalName() + ".preDeleteCluster(), " + "cluster name: " + cinv.getName()
                        + " uuid: " + cinv.getUuid(), e);
            }
        }
    }

    void preChange(ClusterVO vo, ClusterStateEvent event) throws ClusterException {
        ClusterState next = AbstractCluster.getNextState(vo.getState(), event);
        ClusterInventory cinv = ClusterInventory.valueOf(vo);
        for (ClusterChangeStateExtensionPoint extp : changeExts) {
            try {
                extp.preChangeClusterState(cinv, event, next);
            } catch (ClusterException ce) {
                logger.debug(String.format("Extension: %s refused cluster change state operation[ClusterStateEvent:%s] because %s", extp.getClass()
                        .getCanonicalName(), event, ce.getMessage()));
                throw ce;
            } catch (Exception e) {
                logger.warn("Exception happened while calling " + extp.getClass().getCanonicalName() + ".preChangeClusterState(), " + "cluster name: " + cinv.getName()
                        + " uuid: " + cinv.getUuid(), e);
            }
        }
    }

    void preChange(List<ClusterVO> vos, ClusterStateEvent event) throws ClusterException {
        for (ClusterVO vo : vos) {
            preChange(vo, event);
        }
    }

    void beforeChange(ClusterVO vo, final ClusterStateEvent event) {
        final ClusterInventory cinv = ClusterInventory.valueOf(vo);
        final ClusterState next = AbstractCluster.getNextState(vo.getState(), event);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<ClusterChangeStateExtensionPoint>() {
            @Override
            public void run(ClusterChangeStateExtensionPoint extp) {
                extp.beforeChangeClusterState(cinv, event, next);
            }
        });
    }

    void afterChange(ClusterVO vo, final ClusterStateEvent event, final ClusterState prevState) {
        final ClusterInventory cinv = ClusterInventory.valueOf(vo);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<ClusterChangeStateExtensionPoint>() {
            @Override
            public void run(ClusterChangeStateExtensionPoint extp) {
                extp.afterChangeClusterState(cinv, event, prevState);
            }
        });
    }

    void beforeDelete(final ClusterInventory cinv) {
        CollectionUtils.safeForEach(deleteExts, new ForEachFunction<ClusterDeleteExtensionPoint>() {
            @Override
            public void run(ClusterDeleteExtensionPoint arg) {
                arg.beforeDeleteCluster(cinv);
            }
        });
    }

    void afterDelete(final ClusterInventory cinv) {
        CollectionUtils.safeForEach(deleteExts, new ForEachFunction<ClusterDeleteExtensionPoint>() {
            @Override
            public void run(ClusterDeleteExtensionPoint arg) {
                arg.afterDeleteCluster(cinv);
            }
        });
    }

    ErrorCode preUpdateOS(final ClusterVO cls) {
        if (updateOSExts == null || updateOSExts.isEmpty()) {
            return null;
        }

        for (ClusterUpdateOSExtensionPoint ext : updateOSExts) {
            String error = ext.preUpdateClusterOS(cls);
            if (error != null) {
                return operr(error);
            }
        }
        return null;
    }

    void beforeUpdateOS(final ClusterVO cls) {
        CollectionUtils.safeForEach(updateOSExts, new ForEachFunction<ClusterUpdateOSExtensionPoint>() {
            @Override
            public void run(ClusterUpdateOSExtensionPoint arg) {
                arg.beforeUpdateClusterOS(cls);
            }
        });
    }

    void afterUpdateOS(final ClusterVO cls) {
        CollectionUtils.safeForEach(updateOSExts, new ForEachFunction<ClusterUpdateOSExtensionPoint>() {
            @Override
            public void run(ClusterUpdateOSExtensionPoint arg) {
                arg.afterUpdateClusterOS(cls);
            }
        });
    }

    private void populateExtensions() {
        deleteExts = pluginRgty.getExtensionList(ClusterDeleteExtensionPoint.class);
        changeExts = pluginRgty.getExtensionList(ClusterChangeStateExtensionPoint.class);
        updateOSExts = pluginRgty.getExtensionList(ClusterUpdateOSExtensionPoint.class);
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
