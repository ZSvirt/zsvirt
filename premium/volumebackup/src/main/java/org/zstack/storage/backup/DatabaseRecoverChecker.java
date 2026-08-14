package org.zstack.storage.backup;

public interface DatabaseRecoverChecker {
    String getType();
    void check();
}
