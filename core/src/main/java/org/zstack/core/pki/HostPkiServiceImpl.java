package org.zstack.core.pki;

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.encrypt.EncryptFacade;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.pki.ExternalHostPkiExtensionPoint;
import org.zstack.header.pki.HostCertProfile;
import org.zstack.header.pki.HostCertificateBundle;
import org.zstack.header.pki.HostPkiConstant;
import org.zstack.header.pki.HostPkiService;
import org.zstack.header.pki.PkiCaVO;
import org.zstack.header.pki.PkiCaVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.logging.CLogger;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.sql.SQLIntegrityConstraintViolationException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HostPkiServiceImpl implements HostPkiService, Component, PrepareDbInitialValueExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HostPkiServiceImpl.class);
    private static final Object GLOBAL_CA_INIT_LOCK = new Object();
    private static final String CA_SUBJECT_DN = "CN=ZStack Global Host CA";
    private static final int CA_VALIDITY_DAYS = 3650;
    private static final int DEFAULT_HOST_CERT_VALIDITY_DAYS = 365;
    private static final int CRL_NEXT_UPDATE_DAYS = 30;
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EncryptFacade encryptFacade;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void prepareDbInitialValue() {
        // CA initialization moved to first use to avoid dependency on EncryptFacade during bootstrap
    }

    @Override
    @Transactional
    public PkiCaVO ensureGlobalHostCa() {
        PkiCaVO existing = getGlobalHostCa();
        if (existing != null) {
            return existing;
        }

        synchronized (GLOBAL_CA_INIT_LOCK) {
            existing = getGlobalHostCa();
            if (existing != null) {
                return existing;
            }

            try {
                persistBuiltinCa();
            } catch (Exception e) {
                if (!ExceptionDSL.isCausedBy(e, SQLIntegrityConstraintViolationException.class, "Duplicate entry")) {
                    throw new CloudRuntimeException("failed to initialize builtin Host PKI CA", e);
                }
                logger.debug("detected existing builtin Host PKI CA after concurrent initialization");
            }

            existing = waitForGlobalHostCa();
            if (existing == null) {
                throw new CloudRuntimeException("failed to initialize builtin Host PKI CA");
            }
            return existing;
        }
    }

    private PkiCaVO waitForGlobalHostCa() {
        for (int i = 0; i < 5; i++) {
            PkiCaVO ca = getGlobalHostCa();
            if (ca != null) {
                return ca;
            }
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return getGlobalHostCa();
    }

    private void persistBuiltinCa() throws Exception {
        PkiCaVO vo = createBuiltinCa();
        dbf.persistAndRefresh(vo);
        logger.info(String.format("created builtin Host PKI CA[uuid:%s, subject:%s]", vo.getUuid(), vo.getSubjectDn()));
    }

    @Override
    public PkiCaVO getGlobalHostCa() {
        return Q.New(PkiCaVO.class)
                .eq(PkiCaVO_.scope, HostPkiConstant.GLOBAL_SCOPE)
                .eq(PkiCaVO_.caType, HostPkiConstant.CA_TYPE_BUILTIN)
                .eq(PkiCaVO_.status, HostPkiConstant.CA_STATUS_ACTIVE)
                .find();
    }

    @Override
    public HostCertificateBundle signCsr(String csrPem, HostCertProfile profile) {
        if (StringUtils.isBlank(csrPem)) {
            throw new IllegalArgumentException("csrPem cannot be null or empty");
        }

        for (ExternalHostPkiExtensionPoint ext : pluginRgty.getExtensionList(ExternalHostPkiExtensionPoint.class)) {
            if (ext.supports(profile)) {
                return ext.signCsr(csrPem, profile);
            }
        }

        PkiCaVO ca = ensureGlobalHostCa();
        try {
            PKCS10CertificationRequest csr = parseCsr(csrPem);
            verifyCsrSignature(csr);
            X509Certificate caCert = readCertificate(ca.getCertChainPem());
            PrivateKey caPrivateKey = readPrivateKey(encryptFacade.decrypt(ca.getEncryptedPrivateKeyPem()));
            X509Certificate cert = signCertificate(csr, profile, caCert, caPrivateKey);

            HostCertificateBundle bundle = new HostCertificateBundle();
            bundle.setCertificatePem(writePem(cert));
            bundle.setCaCertPem(ca.getCertChainPem());
            bundle.setSerial(cert.getSerialNumber().toString(16));
            bundle.setFingerprint(fingerprint(cert));
            bundle.setNotBefore(new Timestamp(cert.getNotBefore().getTime()));
            bundle.setNotAfter(new Timestamp(cert.getNotAfter().getTime()));
            return bundle;
        } catch (Exception e) {
            throw new CloudRuntimeException("failed to sign host certificate CSR", e);
        }
    }

    @Override
    @Transactional
    public void revokeHostCert(String serial, String reason) {
        List<ExternalHostPkiExtensionPoint> externalExts = pluginRgty.getExtensionList(ExternalHostPkiExtensionPoint.class);
        if (!externalExts.isEmpty()) {
            for (ExternalHostPkiExtensionPoint ext : externalExts) {
                ext.revokeHostCert(serial, reason);
                logger.info(String.format("revoked external host certificate[serial:%s, reason:%s]", serial, reason));
            }
            return;
        }

        try {
            PkiCaVO ca = getGlobalHostCa();
            if (ca == null) {
                throw new CloudRuntimeException("builtin Host PKI CA not found");
            }
            X509Certificate caCert = readCertificate(ca.getCertChainPem());
            PrivateKey caPrivateKey = readPrivateKey(encryptFacade.decrypt(ca.getEncryptedPrivateKeyPem()));

            X500Name issuer = new X500Name(caCert.getSubjectX500Principal().getName(X500Principal.RFC2253));
            Date now = new Date();
            Date nextUpdate = Date.from(now.toInstant().plus(CRL_NEXT_UPDATE_DAYS, ChronoUnit.DAYS));

            X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(issuer, now);
            crlBuilder.setNextUpdate(nextUpdate);

            // carry over existing revoked entries
            if (StringUtils.isNotBlank(ca.getCrlPem())) {
                X509CRLHolder existing = (X509CRLHolder) readPemObject(ca.getCrlPem());
                crlBuilder.addCRL(existing);
            }

            crlBuilder.addCRLEntry(new BigInteger(serial, 16), now, toCrlReasonCode(reason));

            ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(caPrivateKey);
            String crlPem = writePem(crlBuilder.build(signer));

            ca.setCrlPem(crlPem);
            dbf.getEntityManager().merge(ca);
            logger.info(String.format("revoked builtin host certificate[serial:%s, reason:%s]", serial, reason));
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("failed to revoke host certificate[serial:%s]", serial), e);
        }
    }

    @Override
    public String refreshCrl() {
        for (ExternalHostPkiExtensionPoint ext : pluginRgty.getExtensionList(ExternalHostPkiExtensionPoint.class)) {
            String crl = ext.refreshCrl();
            if (StringUtils.isNotBlank(crl)) {
                return crl;
            }
        }

        PkiCaVO ca = getGlobalHostCa();
        return ca != null ? ca.getCrlPem() : null;
    }

    private PkiCaVO createBuiltinCa() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        X509Certificate caCert = generateCaCertificate(keyPair);
        Timestamp now = dbf.getCurrentSqlTime();

        PkiCaVO vo = new PkiCaVO();
        vo.setUuid(Platform.getUuid());
        vo.setScope(HostPkiConstant.GLOBAL_SCOPE);
        vo.setCaType(HostPkiConstant.CA_TYPE_BUILTIN);
        vo.setSubjectDn(CA_SUBJECT_DN);
        vo.setCertChainPem(writePem(caCert));
        vo.setEncryptedPrivateKeyPem(encryptFacade.encrypt(writePem(keyPair.getPrivate())));
        vo.setSerial(caCert.getSerialNumber().toString(16));
        vo.setFingerprint(fingerprint(caCert));
        vo.setStatus(HostPkiConstant.CA_STATUS_ACTIVE);
        vo.setNotBefore(new Timestamp(caCert.getNotBefore().getTime()));
        vo.setNotAfter(new Timestamp(caCert.getNotAfter().getTime()));
        vo.setCreateDate(now);
        vo.setLastOpDate(now);
        return vo;
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair();
    }

    private X509Certificate generateCaCertificate(KeyPair keyPair) throws Exception {
        Instant now = Instant.now().minus(5, ChronoUnit.MINUTES);
        Date notBefore = Date.from(now);
        Date notAfter = Date.from(now.plus(CA_VALIDITY_DAYS, ChronoUnit.DAYS));
        BigInteger serial = new BigInteger(160, new SecureRandom()).abs();
        X500Name subject = new X500Name(CA_SUBJECT_DN);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject,
                serial,
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic()
        );
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        builder.addExtension(Extension.subjectKeyIdentifier, false, extensionUtils.createSubjectKeyIdentifier(keyPair.getPublic()));

        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    private X509Certificate signCertificate(PKCS10CertificationRequest csr, HostCertProfile profile,
                                            X509Certificate caCert, PrivateKey caPrivateKey) throws Exception {
        HostCertProfile effectiveProfile = profile == null ? new HostCertProfile() : profile;
        Instant now = Instant.now().minus(5, ChronoUnit.MINUTES);
        Date notBefore = Date.from(now);
        int validityDays = effectiveProfile.getValidityDays() > 0 ? effectiveProfile.getValidityDays() : DEFAULT_HOST_CERT_VALIDITY_DAYS;
        Date notAfter = Date.from(now.plus(validityDays, ChronoUnit.DAYS));
        Date caNotAfter = caCert.getNotAfter();
        if (notAfter.after(caNotAfter)) {
            notAfter = caNotAfter;
        }

        X500Name subject = StringUtils.isBlank(effectiveProfile.getSubjectDn()) ? csr.getSubject() : new X500Name(effectiveProfile.getSubjectDn());
        SubjectPublicKeyInfo publicKeyInfo = csr.getSubjectPublicKeyInfo();
        X500Name issuer = new X500Name(caCert.getSubjectX500Principal().getName(X500Principal.RFC2253));

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                issuer,
                new BigInteger(160, new SecureRandom()).abs(),
                notBefore,
                notAfter,
                subject,
                publicKeyInfo
        );
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        addExtendedKeyUsage(builder, effectiveProfile.getRoles());
        builder.addExtension(Extension.authorityKeyIdentifier, false, extensionUtils.createAuthorityKeyIdentifier(caCert));
        builder.addExtension(Extension.subjectKeyIdentifier, false, extensionUtils.createSubjectKeyIdentifier(publicKeyInfo));
        addSubjectAlternativeNames(builder, effectiveProfile.getSubjectAlternativeNames());

        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(caPrivateKey);
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    private void addExtendedKeyUsage(X509v3CertificateBuilder builder, List<String> roles) throws Exception {
        List<KeyPurposeId> purposes = new ArrayList<>();
        if (roles != null) {
            for (String role : roles) {
                if (HostPkiConstant.HOST_CERT_ROLE_SERVER.equalsIgnoreCase(role)) {
                    purposes.add(KeyPurposeId.id_kp_serverAuth);
                } else if (HostPkiConstant.HOST_CERT_ROLE_CLIENT.equalsIgnoreCase(role)) {
                    purposes.add(KeyPurposeId.id_kp_clientAuth);
                }
            }
        }

        if (purposes.isEmpty()) {
            purposes.add(KeyPurposeId.id_kp_serverAuth);
            purposes.add(KeyPurposeId.id_kp_clientAuth);
        }

        builder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(purposes.toArray(new KeyPurposeId[0])));
    }

    private void addSubjectAlternativeNames(X509v3CertificateBuilder builder, List<String> subjectAlternativeNames) throws Exception {
        if (subjectAlternativeNames == null || subjectAlternativeNames.isEmpty()) {
            return;
        }

        GeneralName[] names = subjectAlternativeNames.stream()
                .filter(StringUtils::isNotBlank)
                .map(this::toGeneralName)
                .toArray(GeneralName[]::new);
        if (names.length > 0) {
            builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(names));
        }
    }

    private GeneralName toGeneralName(String san) {
        if (san.matches("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$")) {
            return new GeneralName(GeneralName.iPAddress, san);
        }
        if (san.matches("^[0-9a-fA-F:]+$") && san.contains(":")) {
            return new GeneralName(GeneralName.iPAddress, san);
        }
        return new GeneralName(GeneralName.dNSName, san);
    }

    private int toCrlReasonCode(String reason) {
        if (reason == null) return CRLReason.unspecified;
        switch (reason.toLowerCase()) {
            case "keycompromise":       return CRLReason.keyCompromise;
            case "cacompromise":        return CRLReason.cACompromise;
            case "affiliationchanged":  return CRLReason.affiliationChanged;
            case "superseded":          return CRLReason.superseded;
            case "cessationofoperation":return CRLReason.cessationOfOperation;
            case "certificatehold":     return CRLReason.certificateHold;
            case "removefromcrl":       return CRLReason.removeFromCRL;
            default:                    return CRLReason.unspecified;
        }
    }

    private PKCS10CertificationRequest parseCsr(String csrPem) throws IOException {
        Object object = readPemObject(csrPem);
        if (!(object instanceof PKCS10CertificationRequest)) {
            throw new IllegalArgumentException("PEM content is not a PKCS#10 CSR");
        }
        return (PKCS10CertificationRequest) object;
    }

    private void verifyCsrSignature(PKCS10CertificationRequest csr) throws Exception {
        if (!csr.isSignatureValid(new JcaContentVerifierProviderBuilder()
                .build(csr.getSubjectPublicKeyInfo()))) {
            throw new IllegalArgumentException("CSR signature is invalid (proof of possession failed)");
        }
    }

    private X509Certificate readCertificate(String certPem) throws Exception {
        Object object = readPemObject(certPem);
        if (!(object instanceof X509CertificateHolder)) {
            throw new IllegalArgumentException("PEM content is not an X.509 certificate");
        }
        return new JcaX509CertificateConverter().getCertificate((X509CertificateHolder) object);
    }

    private PrivateKey readPrivateKey(String privateKeyPem) throws Exception {
        Object object = readPemObject(privateKeyPem);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        if (object instanceof PEMKeyPair) {
            return converter.getKeyPair((PEMKeyPair) object).getPrivate();
        }
        if (object instanceof PrivateKey) {
            return (PrivateKey) object;
        }
        if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
            return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
        }
        throw new IllegalArgumentException("PEM content is not a private key");
    }

    private Object readPemObject(String pem) throws IOException {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            return parser.readObject();
        }
    }

    private String writePem(Object object) throws IOException {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(object);
        }
        return writer.toString();
    }

    private String fingerprint(X509Certificate certificate) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(certificate.getEncoded());
        StringBuilder builder = new StringBuilder(bytes.length * 3);
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                builder.append(':');
            }
            builder.append(String.format("%02X", bytes[i]));
        }
        return builder.toString();
    }
}
