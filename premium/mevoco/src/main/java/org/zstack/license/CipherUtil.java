package org.zstack.license;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

public class CipherUtil {

    public CipherUtil() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public byte[] rsaDecrypt(InputStream privKeyFile, final String encryptedData)
            throws Exception
    {
        PEMParser pemReader = new PEMParser(new InputStreamReader(privKeyFile));
        PemObject pemObj = pemReader.readPemObject();

        KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(pemObj.getContent());
        PrivateKey key = factory.generatePrivate(privKeySpec);

        // decrypt the text using the private key
        Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(Base64.decodeBase64(encryptedData));
    }

    public byte[] aesDecrypt(final byte[] aeskey, final String b64encrypted)
            throws Exception
    {
        IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(aeskey, 0, 16));
        Cipher cipher = Cipher.getInstance("AES/CFB/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(aeskey, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] data = Base64.decodeBase64(b64encrypted);
        byte[] plainText = new byte[cipher.getOutputSize(data.length)];
        int ptLength = cipher.update(data, 0, data.length, plainText, 0);
        cipher.doFinal(plainText, ptLength);
        return plainText;
    }

    public X509Certificate parseX509Certificate(String base64CertString)
            throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
        InputStream in = new ByteArrayInputStream(java.util.Base64.getDecoder().decode(base64CertString));
        return (X509Certificate) certFactory.generateCertificate(in);
    }
}
