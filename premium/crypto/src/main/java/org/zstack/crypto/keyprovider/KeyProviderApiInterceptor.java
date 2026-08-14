package org.zstack.crypto.keyprovider;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.crypto.keyprovider.api.APICreateKeyProviderMsg;
import org.zstack.crypto.keyprovider.api.APIDeleteKeyProviderMsg;
import org.zstack.crypto.keyprovider.api.APIRekeyKeyProviderRefsMsg;
import org.zstack.crypto.keyprovider.kms.api.APICreateKmsMsg;
import org.zstack.crypto.keyprovider.kms.api.APIGetKmsServerCertFromKmsMsg;
import org.zstack.crypto.keyprovider.kms.api.APIUpdateKmsMsg;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientCsrMsg;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientIdentityMsg;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientSignedCertMsg;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsServerCertMsg;
import org.zstack.crypto.keyprovider.nkp.api.APIParseNkpRestoreMsg;
import org.zstack.crypto.keyprovider.nkp.api.APIRestoreNkpMsg;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.keyprovider.KeyProviderConstant;
import org.zstack.header.keyprovider.KeyProviderErrors;
import org.zstack.header.keyprovider.KeyProviderGlobalConfig;
import org.zstack.header.keyprovider.KeyProviderUtils;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KeyProviderVO_;
import org.zstack.header.keyprovider.KmsIdentityType;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.keyprovider.NkpRestoreInfo;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.CollectionUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

@InterceptorForService("keyProvider")
public class KeyProviderApiInterceptor implements ApiMessageInterceptor {
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
            "^(?=.{1,253}$)(?!-)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)(?:\\.(?!-)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?))*$"
    );

    @Autowired
    private CloudBus bus;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateKeyProviderMsg) {
            validateNameUnique((APICreateKeyProviderMsg) msg);
        }

        if (msg instanceof APICreateKmsMsg) {
            validate((APICreateKmsMsg) msg);
        } else if (msg instanceof APIUpdateKmsMsg) {
            validate((APIUpdateKmsMsg) msg);
        } else if (msg instanceof APIUploadKmsClientIdentityMsg) {
            validate((APIUploadKmsClientIdentityMsg) msg);
        } else if (msg instanceof APIUploadKmsClientCsrMsg) {
            validate((APIUploadKmsClientCsrMsg) msg);
        } else if (msg instanceof APIUploadKmsClientSignedCertMsg) {
            validate((APIUploadKmsClientSignedCertMsg) msg);
        } else if (msg instanceof APIUploadKmsServerCertMsg) {
            validate((APIUploadKmsServerCertMsg) msg);
        } else if (msg instanceof APIGetKmsServerCertFromKmsMsg) {
            validate((APIGetKmsServerCertFromKmsMsg) msg);
        } else if (msg instanceof APIParseNkpRestoreMsg) {
            validate((APIParseNkpRestoreMsg) msg);
        } else if (msg instanceof APIRestoreNkpMsg) {
            validate((APIRestoreNkpMsg) msg);
        } else if (msg instanceof APIRekeyKeyProviderRefsMsg) {
            validate((APIRekeyKeyProviderRefsMsg) msg);
        } else if (msg instanceof APIDeleteKeyProviderMsg) {
            validate((APIDeleteKeyProviderMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof KeyProviderMessage) {
            KeyProviderMessage kmsg = (KeyProviderMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, KeyProviderConstant.SERVICE_ID, kmsg.getKeyProviderUuid());
        }
    }

    private void validateNameUnique(APICreateKeyProviderMsg msg) {
        String name = msg.getName();
        if (StringUtils.isEmpty(name)) {
            return;
        }
        if (KeyProviderUtils.isNameExists(name)) {
            throw new ApiMessageInterceptionException(argerr("key provider name[%s] already exists", name));
        }
    }

    private void validate(APICreateKmsMsg msg) {
        validateEndpoint(msg.getEndpoint());
    }

    private void validateEndpoint(String endpoint) {
        if (StringUtils.isEmpty(endpoint)) {
            return;
        }

        if (!InetAddressValidator.getInstance().isValid(endpoint)
                && !DomainValidator.getInstance(true).isValid(endpoint)
                && !HOSTNAME_PATTERN.matcher(endpoint).matches()) {
            throw new ApiMessageInterceptionException(argerr("kms endpoint[%s] is not valid", endpoint));
        }
    }

    private void validate(APIUpdateKmsMsg msg) {
        validateEndpoint(msg.getEndpoint());
    }

    private void validate(APIUploadKmsClientIdentityMsg msg) {
        KmsIdentityType identityType = KmsIdentityType.valueOf(msg.getIdentityType());
        if (identityType == KmsIdentityType.CSR) {
            throw new ApiMessageInterceptionException(argerr(
                    "identityType[%s] is not allowed in this API, use APIUploadKmsClientCsrMsg to upload CSR",
                    msg.getIdentityType()));
        }
        validateCertificateAndKey(msg.getKmsClientCertPem(), msg.getKmsClientKeyPem());
    }

    private void validate(APIUploadKmsClientCsrMsg msg) {
        PKCS10CertificationRequest csr = parseCsr(msg.getCsrPem(), "csrPem");
        PrivateKey privateKey = parsePrivateKey(msg.getCsrKeyPem(), "csrKeyPem");
        PublicKey publicKey = convertPublicKey(csr.getSubjectPublicKeyInfo(), "csrPem");
        validateKeyMatchesPublicKey(privateKey, publicKey, "csrPem");
    }

    private void validate(APIUploadKmsClientSignedCertMsg msg) {
        parseCertificate(msg.getSignedClientCertPem(), "signedClientCertPem");
    }

    private void validate(APIUploadKmsServerCertMsg msg) {
        parseCertificate(msg.getServerCertPem(), "serverCertPem");
    }

    private void validate(APIGetKmsServerCertFromKmsMsg msg) {
        KmsVO kms = KeyProviderUtils.getKmsByUuid(msg.getUuid());
        if (StringUtils.isEmpty(kms.getEndpoint())) {
            throw new ApiMessageInterceptionException(argerr("kms[uuid:%s] endpoint is empty", msg.getUuid()));
        }
        if (kms.getPort() == null) {
            throw new ApiMessageInterceptionException(argerr("kms[uuid:%s] port is empty", msg.getUuid()));
        }
    }

    private void validate(APIParseNkpRestoreMsg msg) {
        NkpParseResult result = parseRestoreInfo(msg.getContentBase64(), msg.getPassword(), msg.getId());
        msg.setRestoreInfo(result.restoreInfo);
        msg.setParseCode(result.code);
        msg.setParseReason(result.reason);
    }

    private void validate(APIRestoreNkpMsg msg) {
        NkpParseResult result = parseRestoreInfo(msg.getContentBase64(), msg.getPassword(), msg.getId());
        if (result.restoreInfo == null) {
            throw new ApiMessageInterceptionException(argerr(result.reason));
        }
        msg.setRestoreInfo(result.restoreInfo);
    }

    private void validate(APIRekeyKeyProviderRefsMsg msg) {
        try {
            KeyProviderRekeyValidator.getAvailableTargetProvider(msg.getProviderUuid());
        } catch (OperationFailureException e) {
            throw new ApiMessageInterceptionException(e.getErrorCode());
        }

        boolean hasRefIds = !CollectionUtils.isEmpty(msg.getRefIds());
        boolean hasResourceType = StringUtils.isNotBlank(msg.getResourceType());
        boolean hasResourceUuids = !CollectionUtils.isEmpty(msg.getResourceUuids());
        if (hasResourceType != hasResourceUuids) {
            throw new ApiMessageInterceptionException(argerr("resourceType and resourceUuids must be specified together"));
        }

        boolean hasResourceSelector = hasResourceType && hasResourceUuids;
        if (msg.isRekeyAll()) {
            if (hasRefIds || hasResourceSelector) {
                throw new ApiMessageInterceptionException(argerr(
                        "rekeyAll=true cannot be used with refIds or resourceType/resourceUuids"));
            }
            return;
        }

        if (!hasRefIds && !hasResourceSelector) {
            throw new ApiMessageInterceptionException(argerr(
                    "must specify either refIds or resourceType/resourceUuids for rekey, or set rekeyAll=true"));
        }

        if (hasRefIds && hasResourceSelector) {
            throw new ApiMessageInterceptionException(argerr(
                    "refIds and resourceType/resourceUuids are mutually exclusive, specify only one selector"));
        }
    }

    private NkpParseResult parseRestoreInfo(String content, String password, String requestId) {
        KeyToolGrpcClient.ImportNkpResult result;
        try {
            result = KeyToolGrpcClient.parseNkpRestoreInfoWithStatus(requestId, content, password);
        } catch (OperationFailureException e) {
            return NkpParseResult.fail(KeyProviderErrors.BACKEND_UNAVAILABLE, e.getErrorCode().getDetails());
        }
        if (result.getStatusCode() != 0) {
            KeyProviderErrors error = KeyProviderErrors.fromKeyToolStatus(result.getStatusCode());
            return NkpParseResult.fail(error, result.getStatusMessage());
        }
        if (result.getMetaData() == null || result.getMetaData().length == 0) {
            return NkpParseResult.fail(KeyProviderErrors.INVALID_CONTENT, "nkp restore metadata is empty");
        }
        NkpRestoreInfo info;
        try {
            info = NkpRestoreInfo.fromJsonBytes(result.getMetaData());
        } catch (IllegalArgumentException e) {
            return NkpParseResult.fail(KeyProviderErrors.INVALID_CONTENT, e.getMessage());
        }

        if (StringUtils.isEmpty(info.getName())) {
            return NkpParseResult.fail(KeyProviderErrors.INVALID_CONTENT, "nkp restore content missing provider name");
        }
        if (StringUtils.isEmpty(info.getUuid())) {
            return NkpParseResult.fail(KeyProviderErrors.INVALID_CONTENT, "nkp restore content missing provider uuid");
        }
        if (KeyProviderUtils.isNameExists(info.getName())) {
            return NkpParseResult.fail(KeyProviderErrors.NAME_DUPLICATE,
                    String.format("key provider name[%s] already exists", info.getName()));
        }
        if (Q.New(KeyProviderVO.class).eq(KeyProviderVO_.uuid, info.getUuid()).isExists()) {
            return NkpParseResult.fail(KeyProviderErrors.UUID_DUPLICATE,
                    String.format("key provider uuid[%s] already exists", info.getUuid()));
        }
        return NkpParseResult.success(info);
    }

    private void validate(APIDeleteKeyProviderMsg msg) {
        if (KeyProviderUtils.isDefaultKeyProviderUuidEmpty()) {
            return;
        }

        String defaultUuid = KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.value();
        if (!msg.getUuid().equals(defaultUuid)) {
            return;
        }

        boolean hasOtherKeyProvider = Q.New(KeyProviderVO.class).notEq(KeyProviderVO_.uuid, msg.getUuid()).isExists();
        if (hasOtherKeyProvider) {
            throw new ApiMessageInterceptionException(operr(
                    "cannot delete the default key provider[uuid: %s] while other key providers exist; set another default first", msg.getUuid()));
        }

    }

    private void validateCertificateAndKey(String certPem, String keyPem) {
        X509Certificate certificate = parseCertificate(certPem, "kmsClient certificate");
        PrivateKey privateKey = parsePrivateKey(keyPem, "kmsClient private key");
        validateKeyMatchesPublicKey(privateKey, certificate.getPublicKey(), "kmsClient certificate");
    }

    private X509Certificate parseCertificate(String pem, String fieldName) {
        try {
            return KeyProviderUtils.parseCertificate(pem, fieldName);
        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr(e.getMessage()).withException(e.getMessage()));
        }
    }

    private PKCS10CertificationRequest parseCsr(String pem, String fieldName) {
        try {
            return KeyProviderUtils.parseCsr(pem, fieldName);
        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr(e.getMessage()).withException(e.getMessage()));
        }
    }

    private PrivateKey parsePrivateKey(String pem, String fieldName) {
        try {
            return KeyProviderUtils.parsePrivateKey(pem, fieldName);
        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr(e.getMessage()).withException(e.getMessage()));
        }
    }

    private PublicKey convertPublicKey(SubjectPublicKeyInfo info, String fieldName) {
        if (info == null) {
            throw new ApiMessageInterceptionException(argerr("invalid %s format", fieldName));
        }
        KeyProviderUtils.ensureBcProvider();
        try {
            return new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPublicKey(info);
        } catch (Exception e) {
            throw new ApiMessageInterceptionException(argerr("invalid %s format", fieldName).withException(e.getMessage()));
        }
    }

    private void validateKeyMatchesPublicKey(PrivateKey privateKey, PublicKey publicKey, String fieldName) {
        try {
            KeyProviderUtils.validateKeyMatchesPublicKey(privateKey, publicKey, fieldName);
        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr(e.getMessage()).withException(e.getMessage()));
        }
    }

    private static class NkpParseResult {
        private final String code;
        private final String reason;
        private final NkpRestoreInfo restoreInfo;

        private NkpParseResult(String code, String reason, NkpRestoreInfo restoreInfo) {
            this.code = code;
            this.reason = reason;
            this.restoreInfo = restoreInfo;
        }

        private static NkpParseResult success(NkpRestoreInfo restoreInfo) {
            return new NkpParseResult(KeyProviderErrors.OK.toString(), null, restoreInfo);
        }

        private static NkpParseResult fail(KeyProviderErrors error, String reason) {
            String message = StringUtils.isBlank(reason) ? "nkp restore content is invalid" : reason;
            return new NkpParseResult(error.toString(), message, null);
        }
    }
}
