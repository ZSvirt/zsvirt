package org.zstack.header.keyprovider;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.zstack.core.Platform.operr;

public final class KeyProviderUtils {
    public static boolean isDefaultKeyProviderUuidEmpty() {
        String value = KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.value();
        return StringUtils.isEmpty(value) || KeyProviderGlobalConfig.NONE.equals(value);
    }

    public static boolean isNameExists(String name) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        return Q.New(KeyProviderVO.class).eq(KeyProviderVO_.name, name).isExists();
    }

    public static KmsVO getKmsByUuid(String uuid) {
        KmsVO kms = Q.New(KmsVO.class).eq(KmsVO_.uuid, uuid).find();
        if (kms == null) {
            throw new OperationFailureException(operr("cannot find kms[uuid:%s]", uuid));
        }
        return kms;
    }

    public static KmsIdentityVO findKmsIdentity(String kmsUuid, KmsIdentityType identityType) {
        return Q.New(KmsIdentityVO.class)
                .eq(KmsIdentityVO_.kmsUuid, kmsUuid)
                .eq(KmsIdentityVO_.identityType, identityType)
                .find();
    }

    public static String getSyncSignature(KeyProviderType type, String uuid) {
        return String.format("%s-%s", type.toString().toLowerCase(), uuid);
    }

    public static String encodeCertPem(X509Certificate cert) {
        if (cert == null) {
            return null;
        }
        try {
            byte[] der = cert.getEncoded();
            String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
            return "-----BEGIN CERTIFICATE-----\n" + base64 + "\n-----END CERTIFICATE-----";
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to encode kms server certificate").withException(e.getMessage()));
        }
    }

    public static void ensureBcProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static X509Certificate parseCertificate(String pem, String fieldName) {
        if (StringUtils.isBlank(pem)) {
            throw new IllegalArgumentException(String.format("%s cannot be empty", fieldName));
        }
        ensureBcProvider();
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj;
            while ((obj = parser.readObject()) != null) {
                if (obj instanceof X509CertificateHolder) {
                    return new JcaX509CertificateConverter()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .getCertificate((X509CertificateHolder) obj);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("invalid %s format", fieldName), e);
        }
        throw new IllegalArgumentException(String.format("invalid %s format", fieldName));
    }

    public static Timestamp parseCertExpiredDate(String certPem) {
        if (StringUtils.isBlank(certPem)) {
            return null;
        }
        try {
            X509Certificate cert = parseCertificate(certPem, "certificate");
            Date notAfter = cert.getNotAfter();
            return notAfter == null ? null : new Timestamp(notAfter.getTime());
        } catch (IllegalArgumentException e) {
            throw new OperationFailureException(operr("invalid certificate format").withException(e.getMessage()));
        }
    }

    public static PKCS10CertificationRequest parseCsr(String pem, String fieldName) {
        if (StringUtils.isBlank(pem)) {
            throw new IllegalArgumentException(String.format("%s cannot be empty", fieldName));
        }
        ensureBcProvider();
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj;
            while ((obj = parser.readObject()) != null) {
                if (obj instanceof PKCS10CertificationRequest) {
                    return (PKCS10CertificationRequest) obj;
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("invalid %s format", fieldName), e);
        }
        throw new IllegalArgumentException(String.format("invalid %s format", fieldName));
    }

    public static PrivateKey parsePrivateKey(String pem, String fieldName) {
        if (StringUtils.isBlank(pem)) {
            throw new IllegalArgumentException(String.format("%s cannot be empty", fieldName));
        }
        ensureBcProvider();
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            if (obj == null) {
                throw new IllegalArgumentException(String.format("invalid %s format", fieldName));
            }
            if (obj instanceof PEMEncryptedKeyPair || obj instanceof PKCS8EncryptedPrivateKeyInfo) {
                throw new IllegalArgumentException(String.format("encrypted private key is not supported for %s", fieldName));
            }
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            if (obj instanceof PEMKeyPair) {
                return converter.getKeyPair((PEMKeyPair) obj).getPrivate();
            }
            if (obj instanceof PrivateKeyInfo) {
                return converter.getPrivateKey((PrivateKeyInfo) obj);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("invalid %s format", fieldName), e);
        }
        throw new IllegalArgumentException(String.format("invalid %s format", fieldName));
    }

    public static void validateKeyMatchesPublicKey(PrivateKey privateKey, PublicKey publicKey, String fieldName) {
        if (privateKey == null || publicKey == null) {
            throw new IllegalArgumentException(String.format("invalid %s format", fieldName));
        }
        if (!privateKey.getAlgorithm().equalsIgnoreCase(publicKey.getAlgorithm())) {
            throw new IllegalArgumentException(String.format(
                    "private key algorithm[%s] does not match public key algorithm[%s] for %s",
                    privateKey.getAlgorithm(), publicKey.getAlgorithm(), fieldName));
        }
        String signatureAlgorithm = resolveSignatureAlgorithm(privateKey.getAlgorithm());
        if (signatureAlgorithm == null) {
            throw new IllegalArgumentException(String.format(
                    "unsupported private key algorithm[%s] for %s", privateKey.getAlgorithm(), fieldName));
        }
        try {
            Signature signature = Signature.getInstance(signatureAlgorithm);
            byte[] data = "zstack-key-provider-validate".getBytes(StandardCharsets.UTF_8);
            signature.initSign(privateKey);
            signature.update(data);
            byte[] signed = signature.sign();
            signature.initVerify(publicKey);
            signature.update(data);
            if (!signature.verify(signed)) {
                throw new IllegalArgumentException(String.format("private key does not match public key for %s", fieldName));
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("failed to validate %s", fieldName), e);
        }
    }

    private static String resolveSignatureAlgorithm(String keyAlgorithm) {
        if (StringUtils.isBlank(keyAlgorithm)) {
            return null;
        }
        switch (keyAlgorithm.toUpperCase(Locale.ENGLISH)) {
            case "RSA":
                return "SHA256withRSA";
            case "EC":
            case "ECDSA":
                return "SHA256withECDSA";
            case "DSA":
                return "SHA256withDSA";
            case "ED25519":
                return "Ed25519";
            case "ED448":
                return "Ed448";
            default:
                return null;
        }
    }

    public static void validateCertificateMatchesEndpoint(X509Certificate cert, String endpoint) {
        if (cert == null) {
            throw new IllegalArgumentException("server certificate is empty");
        }
        if (StringUtils.isBlank(endpoint)) {
            throw new IllegalArgumentException("endpoint is empty");
        }
        boolean isIp = InetAddressValidator.getInstance().isValid(endpoint);
        boolean matched = false;
        boolean hasRelevantSan = false;
        Collection<List<?>> subjectAltNames = getSubjectAltNames(cert);
        if (subjectAltNames != null) {
            for (List<?> entry : subjectAltNames) {
                if (entry == null || entry.size() < 2) {
                    continue;
                }
                Object typeObj = entry.get(0);
                Object valueObj = entry.get(1);
                if (!(typeObj instanceof Integer)) {
                    continue;
                }
                int type = (Integer) typeObj;
                if (isIp && type == 7) {
                    hasRelevantSan = true;
                    if (valueObj == null) {
                        continue;
                    }
                    String value = valueObj.toString();
                    if (endpoint.equalsIgnoreCase(value)) {
                        matched = true;
                        break;
                    }
                } else if (!isIp && type == 2) {
                    hasRelevantSan = true;
                    if (valueObj == null) {
                        continue;
                    }
                    String value = valueObj.toString();
                    if (matchDns(value, endpoint)) {
                        matched = true;
                        break;
                    }
                }
            }
        }
        if (!matched && !hasRelevantSan) {
            String commonName = getCommonName(cert);
            if (!StringUtils.isBlank(commonName)) {
                if (isIp) {
                    matched = endpoint.equalsIgnoreCase(commonName);
                } else {
                    matched = matchDns(commonName, endpoint);
                }
            }
        }
        if (!matched) {
            throw new IllegalArgumentException(String.format("server certificate does not match endpoint[%s]", endpoint));
        }
    }

    public static boolean isSelfSignedCertificate(X509Certificate cert) {
        if (cert == null) {
            return false;
        }
        try {
            cert.verify(cert.getPublicKey());
            return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
        } catch (Exception ignored) {
            return false;
        }
    }

    public static CertificateInfo parseCertificateInfo(String certPem) {
        if (StringUtils.isBlank(certPem)) {
            return null;
        }
        try {
            X509Certificate cert = parseCertificate(certPem, "certificate");
            return getCertificateInfo(cert);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static CertificateInfo getCertificateInfo(X509Certificate cert) {
        if (cert == null) {
            return null;
        }
        List<String> dns = new ArrayList<>();
        List<String> ip = new ArrayList<>();
        Collection<List<?>> sans = getSubjectAltNames(cert);
        if (sans != null) {
            for (List<?> entry : sans) {
                if (entry == null || entry.size() < 2) {
                    continue;
                }
                Object typeObj = entry.get(0);
                Object valueObj = entry.get(1);
                if (!(typeObj instanceof Integer) || valueObj == null) {
                    continue;
                }
                int type = (Integer) typeObj;
                String value = String.valueOf(valueObj);
                if (type == 2) {
                    dns.add(value);
                } else if (type == 7) {
                    ip.add(value);
                }
            }
        }
        String subject = cert.getSubjectX500Principal() == null ? null : cert.getSubjectX500Principal().getName();
        String issuer = cert.getIssuerX500Principal() == null ? null : cert.getIssuerX500Principal().getName();
        Date notAfter = cert.getNotAfter();
        Timestamp expiredDate = notAfter == null ? null : new Timestamp(notAfter.getTime());
        return new CertificateInfo(subject, issuer, getCommonName(cert), dns, ip, expiredDate);
    }

    private static Collection<List<?>> getSubjectAltNames(X509Certificate cert) {
        try {
            return cert.getSubjectAlternativeNames();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getCommonName(X509Certificate cert) {
        try {
            LdapName ldapName = new LdapName(cert.getSubjectX500Principal().getName());
            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return rdn.getValue().toString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean matchDns(String pattern, String endpoint) {
        if (StringUtils.isBlank(pattern) || StringUtils.isBlank(endpoint)) {
            return false;
        }
        String normalizedPattern = pattern.toLowerCase(Locale.ENGLISH);
        String normalizedEndpoint = endpoint.toLowerCase(Locale.ENGLISH);
        if (normalizedPattern.startsWith("*.")) {
            String suffix = normalizedPattern.substring(1);
            return normalizedEndpoint.endsWith(suffix) && normalizedEndpoint.length() > suffix.length();
        }
        return normalizedEndpoint.equals(normalizedPattern);
    }
}
