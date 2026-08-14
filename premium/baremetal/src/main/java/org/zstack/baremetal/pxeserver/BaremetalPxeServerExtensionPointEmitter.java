package org.zstack.baremetal.pxeserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerAttachExtensionPoint;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerDetachExtensionPoint;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerInventory;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created by GuoYi on 2018-10-30.
 */
public class BaremetalPxeServerExtensionPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(BaremetalPxeServerExtensionPointEmitter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private List<BaremetalPxeServerAttachExtensionPoint> attachExts;
    private List<BaremetalPxeServerDetachExtensionPoint> detachExts;

    void preAttach(BaremetalPxeServerVO vo, String clusterUuid) {
        BaremetalPxeServerInventory inv = BaremetalPxeServerInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, arg -> arg.preAttachBaremetalPxeServer(inv, clusterUuid));
    }

    void beforeAttach(BaremetalPxeServerVO vo, final String clusterUuid) {
        final BaremetalPxeServerInventory inv = BaremetalPxeServerInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, arg -> arg.beforeAttachBaremetalPxeServer(inv, clusterUuid));
    }

    void failToAttach(BaremetalPxeServerVO vo, final String clusterUuid) {
        final BaremetalPxeServerInventory inv = BaremetalPxeServerInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, arg -> arg.failToAttachBaremetalPxeServer(inv, clusterUuid));
    }

    void afterAttach(BaremetalPxeServerVO vo, final String clusterUuid) {
        final BaremetalPxeServerInventory inv = BaremetalPxeServerInventory.valueOf(vo);
        CollectionUtils.safeForEach(attachExts, arg -> arg.afterAttachBaremetalPxeServer(inv, clusterUuid));
    }

    void preDetach(BaremetalPxeServerVO vo, String clusterUuid) {
        BaremetalPxeServerInventory inv = BaremetalPxeServerInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, arg -> arg.preDetachBaremetalPxeServer(inv, clusterUuid));
    }

    void beforeDetach(BaremetalPxeServerVO vo, final String clusterUuid) {
        final BaremetalPxeServerInventory inv = BaremetalPxeServerInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, arg -> arg.beforeDetachBaremetalPxeServer(inv, clusterUuid));
    }

    void failToDetach(BaremetalPxeServerVO vo, final String clusterUuid) {
        final BaremetalPxeServerInventory inv = BaremetalPxeServerInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, arg -> arg.failToDetachBaremetalPxeServer(inv, clusterUuid));
    }

    void afterDetach(BaremetalPxeServerVO vo, final String clusterUuid) {
        final BaremetalPxeServerInventory inv = BaremetalPxeServerInventory.valueOf(vo);
        CollectionUtils.safeForEach(detachExts, arg -> arg.afterDetachBaremetalPxeServer(inv, clusterUuid));
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        attachExts = pluginRgty.getExtensionList(BaremetalPxeServerAttachExtensionPoint.class);
        detachExts = pluginRgty.getExtensionList(BaremetalPxeServerDetachExtensionPoint.class);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
