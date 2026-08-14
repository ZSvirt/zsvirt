package org.zstack.header.bonding;

import org.zstack.header.message.DeletionMessage;

public class BondingDeletionMsg extends DeletionMessage implements BondingMessage {
    String bondingUuid;
    boolean dbOnly;

    @Override
    public String getBondingUuid() {
        return bondingUuid;
    }

    public void setBondingUuid(String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }

    public boolean isDbOnly() {
        return dbOnly;
    }

    public void setDbOnly(boolean dbOnly) {
        this.dbOnly = dbOnly;
    }
}
