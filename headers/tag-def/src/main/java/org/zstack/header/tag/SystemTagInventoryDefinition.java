package org.zstack.header.tag;

import java.sql.Timestamp;

/**
 * Narrow view of a system tag inventory for tag-def APIs that must not depend on header VO/inventory impl.
 */
public interface SystemTagInventoryDefinition {
    String getUuid();

    String getResourceUuid();

    String getResourceType();

    String getTag();

    String getType();

    boolean isInherent();

    Timestamp getCreateDate();

    Timestamp getLastOpDate();
}
