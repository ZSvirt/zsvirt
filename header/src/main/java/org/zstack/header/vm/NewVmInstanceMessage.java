package org.zstack.header.vm;

import java.util.List;

public interface NewVmInstanceMessage {
    String getName();
    String getDescription();
    List<String> getL3NetworkUuids();
    String getDefaultL3NetworkUuid();
    String getType();
    List<String> getSystemTags();
    default String getVmNicParams() {
        return null;
    }
    default String getPlatform() {
        return null;
    }
}
