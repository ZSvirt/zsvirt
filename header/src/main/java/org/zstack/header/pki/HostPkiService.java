package org.zstack.header.pki;

public interface HostPkiService {
    PkiCaVO ensureGlobalHostCa();

    HostCertificateBundle signCsr(String csrPem, HostCertProfile profile);

    void revokeHostCert(String serial, String reason);

    String refreshCrl();

    PkiCaVO getGlobalHostCa();
}
