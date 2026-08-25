package org.zstack.header.pki;

import org.zstack.header.core.Completion;

public interface HostCertificateService {
    void ensureProfile(String hostUuid, String usage, Completion completion);

    void renew(String hostUuid, String usage, Completion completion);

    void revoke(String hostUuid, String usage, String reason, Completion completion);

    void refreshStatus(String hostUuid, String usage, Completion completion);

    boolean isReady(String hostUuid, String usage);

    HostCertificateVO getCertificateMetadata(String hostUuid, String usage);
}
