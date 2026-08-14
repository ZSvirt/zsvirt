package org.zstack.header.tag;

import java.util.Collection;

public interface SystemTagResourceDeletionOperator {
    void execute(Collection resourceUuids);
}
