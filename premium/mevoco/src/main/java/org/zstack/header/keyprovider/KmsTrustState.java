package org.zstack.header.keyprovider;

public enum KmsTrustState {
    MUTUAL_UNTRUSTED,
    MN_TRUSTS_KMS_ONLY,
    KMS_TRUSTS_MN_ONLY,
    MUTUAL_TRUSTED;
}
