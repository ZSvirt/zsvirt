package org.zstack.simulator2.config.primaryStorage

import org.zstack.simulator2.config.Col

class SharedBlockGroupPrimaryStorage extends PrimaryStorage {
    @Col
    String sharedBlockGroupType
    @Col
    String path
}
