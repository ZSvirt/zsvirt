package org.zstack.header.pki;

public interface ExternalHostPkiExtensionPoint {
    boolean supports(HostCertProfile profile);

    HostCertificateBundle signCsr(String csrPem, HostCertProfile profile);

    void revokeHostCert(String serial, String reason);

    String refreshCrl();
}
