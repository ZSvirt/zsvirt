package org.zstack.storage.primary.imagestore.ceph;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.storage.volume.block.BlockPrimaryStorageFactory;

import java.util.HashMap;
import java.util.Map;

public class CephBlockPrimaryStorageManagerImpl implements CephBlockPrimaryStorageManager, Component {
    @Autowired
    private PluginRegistry pluginRgty;

    private Map<String, BlockPrimaryStorageFactory> blockPrimaryStorageFactories = new HashMap<>();

    @Override
    public BlockPrimaryStorageFactory getBlockPrimaryStorageFactory(String type) {
        BlockPrimaryStorageFactory factory = blockPrimaryStorageFactories.get(type);
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find BlockPrimaryStorageFactory with type[%s]", type));
        }

        return factory;
    }

    @Override
    public boolean start() {
        populateExtensions();

        return true;
    }

    private void populateExtensions() {
        for (BlockPrimaryStorageFactory f : pluginRgty.getExtensionList(BlockPrimaryStorageFactory.class)) {
            BlockPrimaryStorageFactory old = blockPrimaryStorageFactories.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate BlockPrimaryStorageFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getType()));
            }
            blockPrimaryStorageFactories.put(f.getType().toString(), f);
        }
    }

    @Override
    public boolean stop() {
        return true;
    }
}
