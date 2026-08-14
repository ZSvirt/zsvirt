package org.zstack.core.db;

import java.util.Collection;
import java.util.List;

/**
 */
public interface HardDeleteEntityExtensionPoint {
    List<Class> getEntityClassForHardDeleteEntityExtension();

    void postHardDelete(Collection entityIds, Class entityClass);
}
