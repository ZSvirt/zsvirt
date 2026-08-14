package org.zstack.storage.volume.block;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * @author shenjin
 * @date 2023/6/20 16:30
 */
public class XskyPrimaryStorageBackendFactory implements Component, BlockPrimaryStorageFactory {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRegistry;
    public Map<String, BlockVolumeBackend> blockVolumeBackends = Collections.synchronizedMap(new HashMap<>());

    @Override
    public String getType() {
        return CephConstants.CEPH_MANUFACTURER_XSKY;
    }

    @Override
    public BlockPrimaryStorageBackend getBlockPrimaryStorageBackend(PrimaryStorageVO vo) {
        return new XskyPrimaryStorageBackend(vo);
    }
    
    private String buildEmptyVolumeInstallPath(String targetCephPoolName,String canonicalPath, String installPath) {
        if (StringUtils.isEmpty(installPath)) {
            return canonicalPath;
        }

        return makeVolumeInstallPathByTargetPool(installPath, targetCephPoolName);
    }

    private String getTargetPoolNameFromAllocatedUrl(String allocatedUrl) {
        if (allocatedUrl == null) {
            throw new OperationFailureException(operr("allocated url not found"));
        }


        if (!allocatedUrl.startsWith("ceph://")) {
            throw new OperationFailureException(operr("invalid allocated url:%s", allocatedUrl));
        }

        String path = allocatedUrl.replaceFirst("ceph://", "");
        return path.substring(0, path.lastIndexOf("/"));
    }

    private String makeVolumeInstallPathByTargetPool(String volUuid, String targetPoolName) {
        return String.format("ceph://%s/%s", targetPoolName, volUuid);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    private void populateExtensions() {
        for (BlockVolumeBackend backend : pluginRegistry.getExtensionList(BlockVolumeBackend.class)) {
            BlockVolumeBackend old = blockVolumeBackends.get(backend.getType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate BlockVolumeBackend[%s, %s] for type[%s]",
                        backend.getClass().getName(), old.getClass().getName(), backend.getType()));
            }
            blockVolumeBackends.put(backend.getType(), backend);
        }
    }
}
