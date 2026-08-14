package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.storage.primary.preallocation.PreallocationFactory;

import java.util.HashMap;
import java.util.Map;

public class PrimaryStorageProvisioningStrategyGetter {
    private static PluginRegistry pluginRgty;

    private static Map<String, PreallocationFactory> preallocationFactories;

    public synchronized static void initPreallocationFactories() {
        if (preallocationFactories == null) {
            preallocationFactories = new HashMap<>();
            if (pluginRgty == null) {
                pluginRgty = Platform.getComponentLoader().getComponent(PluginRegistry.class);
            }
            for (PreallocationFactory f : pluginRgty.getExtensionList(PreallocationFactory.class)) {
                PreallocationFactory old = preallocationFactories.get(f.getPrimaryStorageType());
                if (old != null) {
                    throw new CloudRuntimeException(String.format("duplicate PreallocationFactory[%s, %s] for type[%s]",
                            f.getClass().getName(), old.getClass().getName(), old.getPrimaryStorageType()));
                }
                preallocationFactories.put(f.getPrimaryStorageType(), f);
            }
        }
    }

    public static String get(String primaryStorageUuid) {
        initPreallocationFactories();

        String type = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, primaryStorageUuid)
                .select(PrimaryStorageVO_.type).findValue();

        PreallocationFactory factory = preallocationFactories.get(type);
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No PreallocationFactory for type: %s found", type));
        }
        return factory.getProvisioningStrategy(primaryStorageUuid);
    }
}
