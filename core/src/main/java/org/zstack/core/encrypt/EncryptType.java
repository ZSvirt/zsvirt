package org.zstack.core.encrypt;

/**
 * Created by LiangHanYu on 2021/11/15 13:28
 */
public enum EncryptType {
    RSA("RSA"),
    DIGEST("DIGEST"),
    DIGEST_LOCAL("DIGEST_LOCAL"),
    SM4("SM4"),
    HMAC("HMAC");

    private String name;

    EncryptType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
