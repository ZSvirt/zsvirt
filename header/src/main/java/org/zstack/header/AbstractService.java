package org.zstack.header;

import java.util.List;

/**
 */
public abstract class AbstractService implements Service {
    @Override
    public int getSyncLevel() {
        // NOTE: think twice if you want to change this value to 1.
        // sync delivery will cause deadlock if a service sends message in its message handler
        // to itself
        return 0;
    }

    @Override
    public List<String> getAliasIds() {
        return null;
    }
}
