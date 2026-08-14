package org.zstack.simulator2.config.primaryStorage

import org.zstack.simulator2.config.Col

class MiniStorage extends PrimaryStorage {
    @Col
    String diskIdentifier
    @Col
    String miniStorageType
}
