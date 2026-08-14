package org.zstack.accessKey;

public interface CreateAccessKey {
    String getResourceUuid();

    String getAccountUuid();

    String getUserUuid();

    String getDescription();

    String getAccessKeyID();

    String getAccessKeySecret();
}
