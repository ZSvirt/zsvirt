package org.zstack.header.volume;

import java.io.Serializable;

public class VolumeLuksAgentSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    private String encryptLuksSecretMaterialFilePath;

    public boolean isComplete() {
        return encryptLuksSecretMaterialFilePath != null && !encryptLuksSecretMaterialFilePath.isEmpty();
    }

    public String getEncryptLuksSecretMaterialFilePath() {
        return encryptLuksSecretMaterialFilePath;
    }

    public void setEncryptLuksSecretMaterialFilePath(String encryptLuksSecretMaterialFilePath) {
        this.encryptLuksSecretMaterialFilePath = encryptLuksSecretMaterialFilePath;
    }
}
