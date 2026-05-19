package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public enum AccountType {
    SystemAdmin,
    Normal,
    /**
     * @deprecated Use {@link AccountType#Normal} with {@link AccountSource} instead (ZSV-12257).
     */
    @Deprecated
    ThirdParty
}
