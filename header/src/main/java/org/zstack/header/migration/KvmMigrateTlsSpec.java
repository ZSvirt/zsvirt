package org.zstack.header.migration;

import java.io.Serializable;

public class KvmMigrateTlsSpec implements Serializable {
    private boolean migrateTls;
    private boolean mutualTls = true;
    private String expectedDestCertFingerprint;
    // Opt-in TLS: only Boolean.TRUE enables migration TLS; false/null = plain migration.
    private Boolean requestedByUser;

    public boolean isMigrateTls() {
        return migrateTls;
    }

    public void setMigrateTls(boolean migrateTls) {
        this.migrateTls = migrateTls;
    }

    public boolean isMutualTls() {
        return mutualTls;
    }

    public void setMutualTls(boolean mutualTls) {
        this.mutualTls = mutualTls;
    }

    public String getExpectedDestCertFingerprint() {
        return expectedDestCertFingerprint;
    }

    public void setExpectedDestCertFingerprint(String expectedDestCertFingerprint) {
        this.expectedDestCertFingerprint = expectedDestCertFingerprint;
    }

    public Boolean getRequestedByUser() {
        return requestedByUser;
    }

    public void setRequestedByUser(Boolean requestedByUser) {
        this.requestedByUser = requestedByUser;
    }
}
