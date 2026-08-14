package org.zstack.core.db;

import java.util.Collection;
import java.util.List;

/**
 */
public interface SoftDeleteEntityExtensionPoint {
    List<Class> getEntityClassForSoftDeleteEntityExtension();

    void postSoftDelete(Collection entityIds, Class entityClass);
}
