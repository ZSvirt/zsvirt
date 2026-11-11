package org.zstack.core.privatekey;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.privatekey.PrivateKeyConstant;
import org.zstack.utils.ssh.Ed25519KeyPairs;

import java.io.File;
import java.io.IOException;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class Ed25519KeyClient {
    public String ensureCommonsKeyPair() throws IOException {
        File folder = new File(PrivateKeyConstant.PRIVATE_KEY_FOLDER);
        File privateKey = new File(folder, PrivateKeyConstant.COMMONS_PRIVATE_KEY_PATH);
        File publicKey = new File(folder, PrivateKeyConstant.COMMONS_PUBLIC_KEY_PATH);
        return Ed25519KeyPairs.ensure(privateKey, publicKey);
    }
}
