package org.zstack.header.tpm.api;

public interface TpmMessage {
    String getVmInstanceUuid();
    void setVmInstanceUuid(String vmInstanceUuid);
    String getTpmUuid();
    void setTpmUuid(String tpmUuid);
}
