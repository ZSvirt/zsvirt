package org.zstack.core.db;

import com.mchange.v2.c3p0.AbstractConnectionCustomizer;

import java.sql.Connection;

/**
 */
public class TransactionIsolationLevelSetter extends AbstractConnectionCustomizer {
    @Override
    public void onAcquire(Connection c, String parentDataSourceIdentityToken) throws Exception {
        super.onAcquire(c, parentDataSourceIdentityToken);
        c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    }
}
