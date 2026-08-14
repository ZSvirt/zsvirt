package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public enum AccountType {
    SystemAdmin,
    Normal,
    ThirdParty
}
