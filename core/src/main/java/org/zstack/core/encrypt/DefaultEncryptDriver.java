package org.zstack.core.encrypt;

import org.zstack.header.core.encrypt.EncryptConstant;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.exception.CloudRuntimeException;

public class DefaultEncryptDriver implements EncryptDriver {
    private EncryptDriverType type = new EncryptDriverType(EncryptConstant.DEFAULT);

    public static EncryptRSA rsa = new EncryptRSA();

    @Override
    public EncryptDriverType getDriverType() {
        return type;
    }

    @Override
    public String encrypt(String data) {
        try {
            return rsa.encrypt1(data);
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public String decrypt(String data) {
        try {
            return (String) rsa.decrypt1(data);
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public ErrorableValue<String> encrypt(String data, String algType) {
        try {
            return ErrorableValue.of(rsa.encrypt(data, algType));
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public ErrorableValue<String> decrypt(String data, String algType) {
        try {
            return ErrorableValue.of(rsa.decrypt(data, algType));
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }
}
