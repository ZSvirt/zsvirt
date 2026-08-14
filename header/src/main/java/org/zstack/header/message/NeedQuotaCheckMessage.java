package org.zstack.header.message;

public interface NeedQuotaCheckMessage {
    String getAccountUuid();

    void setAccountUuid(String accountUuid);
}
