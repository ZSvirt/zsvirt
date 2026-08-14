package org.zstack.core.db;

import com.mchange.v2.c3p0.util.IsValidOnlyConnectionTester;

public class C3p0ConnectionTester extends IsValidOnlyConnectionTester {
    @Override
    protected int getIsValidTimeout() {
        return DbGlobalProperty.C3P0_IS_VALID_TIMEOUT;
    }
}
