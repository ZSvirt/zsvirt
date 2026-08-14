package org.zstack.crypto.keyprovider;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.keyprovider.KeyProviderErrors;
import org.zstack.header.keyprovider.KeyProviderGlobalConfig;
import org.zstack.header.keyprovider.NkpRestoreInfo;
import org.zstack.header.keyprovider.NkpVO;
import org.zstack.utils.gson.JSONObjectUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

public final class KeyToolGrpcClient {
    private static final int KEY_TOOL_TIMEOUT_MS = 10000;
    private static final String SERVICE_NAME = "keytools.v1.BackendService";
    private static final String SECRET_SERVICE_NAME = "keytools.v1.SecretService";
    private static final MethodDescriptor<byte[], byte[]> PACK_NKP_METHOD = buildUnaryMethod("PackNKPBackend");
    private static final MethodDescriptor<byte[], byte[]> IMPORT_NKP_METHOD = buildUnaryMethod("ImportNKPBackend");
    private static final MethodDescriptor<byte[], byte[]> INIT_NKP_METHOD = buildUnaryMethod("InitNKPBackend");
    private static final MethodDescriptor<byte[], byte[]> REMOVE_NKP_METHOD = buildUnaryMethod("RemoveNKPBackend");
    private static final MethodDescriptor<byte[], byte[]> HEALTH_NKP_METHOD = buildUnaryMethod("HealthNKPBackend");
    private static final MethodDescriptor<byte[], byte[]> HEALTH_KMIP_METHOD = buildUnaryMethod("HealthKMIPBackend");
    private static final MethodDescriptor<byte[], byte[]> CREATE_SECRET_METHOD = buildUnaryMethod(SECRET_SERVICE_NAME, "CreateSecret");
    private static final MethodDescriptor<byte[], byte[]> REKEY_SECRET_METHOD = buildUnaryMethod(SECRET_SERVICE_NAME, "RekeySecret");
    private static final MethodDescriptor<byte[], byte[]> DELETE_SECRET_METHOD = buildUnaryMethod(SECRET_SERVICE_NAME, "DeleteSecret");
    private static final MethodDescriptor<byte[], byte[]> GET_SECRET_METHOD = buildUnaryMethod(SECRET_SERVICE_NAME, "GetSecret");
    private static final Map<String, ManagedChannel> CHANNEL_CACHE = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(KeyToolGrpcClient::shutdownAllChannels));
    }

    private KeyToolGrpcClient() {
    }

    public static String packNkp(String requestId, String name, String uuid, String password, NkpRestoreInfo restoreInfo) {
        String reqId = normalizeRequestId(requestId);
        byte[] metaData = JSONObjectUtil.toJsonString(restoreInfo).getBytes(StandardCharsets.UTF_8);
        byte[] request = buildPackNkpRequest(reqId, name, uuid, password, metaData);
        PackNkpResponse response = callPackNkp(request);
        if (response.statusCode != 0) {
            throw new OperationFailureException(operr("key-tool pack nkp failed: %s", response.statusMessage));
        }
        if (StringUtils.isEmpty(response.backupData)) {
            throw new OperationFailureException(operr("key-tool pack nkp failed: empty backup data"));
        }
        return response.backupData;
    }

    public static String exportNkpRootKey(String requestId, NkpVO nkp) {
        if (nkp == null) {
            throw new OperationFailureException(operr("nkp is null when exporting root key"));
        }
        return packNkp(requestId, nkp.getName(), nkp.getUuid(), null, NkpRestoreInfo.valueOf(nkp));
    }

    public static ImportNkpResult parseNkpRestoreInfoWithStatus(String requestId, String contentBase64, String password) {
        ImportNkpResponse response = importNkpWithStatus(requestId, contentBase64, password, false);
        return new ImportNkpResult(response.statusCode, response.statusMessage, response.metaData);
    }

    public static void importNkp(String requestId, String contentBase64, String password) {
        ImportNkpResponse response = importNkp(requestId, contentBase64, password, true);
        if (response.metaData == null || response.metaData.length == 0) {
            throw new OperationFailureException(operr("key-tool import nkp failed: empty metadata"));
        }
    }

    public static void importNkpRootKey(String requestId, String contentBase64) {
        importNkp(requestId, contentBase64, null, true);
    }

    public static void initNkp(String requestId, String name, String uuid) {
        String reqId = normalizeRequestId(requestId);
        byte[] request = buildHealthNkpRequest(reqId, name, uuid);
        HealthResult response = parseHealthResponse(callUnary(INIT_NKP_METHOD, request));
        if (response.statusCode != 0) {
            throw new OperationFailureException(operr("key-tool init nkp failed: %s", response.statusMessage));
        }
    }

    public static void removeNkp(String requestId, String name, String uuid) {
        String reqId = normalizeRequestId(requestId);
        byte[] request = buildHealthNkpRequest(reqId, name, uuid);
        HealthResult response = parseHealthResponse(callUnary(REMOVE_NKP_METHOD, request));
        if (response.statusCode != 0) {
            throw new OperationFailureException(operr("key-tool remove nkp failed: %s", response.statusMessage));
        }
    }

    public static HealthResult healthNkp(String requestId, String name, String uuid) {
        String reqId = normalizeRequestId(requestId);
        byte[] request = buildHealthNkpRequest(reqId, name, uuid);
        return parseHealthResponse(callUnary(HEALTH_NKP_METHOD, request));
    }

    public static HealthResult healthKmip(String requestId, KmipConfig kmipConfig) {
        String reqId = normalizeRequestId(requestId);
        byte[] request = buildHealthKmipRequest(reqId, kmipConfig);
        return parseHealthResponse(callUnary(HEALTH_KMIP_METHOD, request));
    }

    public static String rekeySecret(String requestId, String secretRef, String name,
                                     NkpConfig targetNkp, KmipConfig targetKmip,
                                     NkpConfig originNkp, KmipConfig originKmip) {
        String reqId = normalizeRequestId(requestId);
        byte[] request = buildRekeySecretRequest(reqId, secretRef, name, targetNkp, targetKmip, originNkp, originKmip);
        RekeySecretResponse response = parseRekeySecretResponse(callUnary(REKEY_SECRET_METHOD, request));
        if (response.statusCode != 0) {
            throw new OperationFailureException(operr("key-tool rekey failed: %s", response.statusMessage));
        }
        if (StringUtils.isBlank(response.newSecretRef)) {
            throw new OperationFailureException(operr("key-tool rekey failed: empty new secret ref"));
        }
        return response.newSecretRef;
    }

    public static String createSecret(String requestId, String name, NkpConfig targetNkp, KmipConfig targetKmip) {
        String reqId = normalizeRequestId(requestId);
        byte[] request = buildCreateSecretRequest(reqId, name, targetNkp, targetKmip);
        CreateSecretResponse response = parseCreateSecretResponse(callUnary(CREATE_SECRET_METHOD, request));
        if (response.statusCode != 0) {
            throw new OperationFailureException(operr("key-tool create secret failed: %s", response.statusMessage));
        }
        if (StringUtils.isBlank(response.secretRef)) {
            throw new OperationFailureException(operr("key-tool create secret failed: empty secret ref"));
        }
        return response.secretRef;
    }

    public static byte[] getSecret(String requestId, String secretRef, NkpConfig targetNkp, KmipConfig targetKmip) {
        String reqId = normalizeRequestId(requestId);
        byte[] request = buildGetSecretRequest(reqId, secretRef, targetNkp, targetKmip);
        GetSecretResponse response = parseGetSecretResponse(callUnary(GET_SECRET_METHOD, request));
        if (response.statusCode != 0) {
            throw new OperationFailureException(operr("key-tool get secret failed: %s", response.statusMessage));
        }
        if (response.payload == null || response.payload.length == 0) {
            throw new OperationFailureException(operr("key-tool get secret failed: empty payload"));
        }
        return response.payload;
    }

    public static void deleteSecret(String requestId, String secretRef, NkpConfig targetNkp, KmipConfig targetKmip) {
        String reqId = normalizeRequestId(requestId);
        byte[] request = buildDeleteSecretRequest(reqId, secretRef, targetNkp, targetKmip);
        DeleteSecretResponse response = parseDeleteSecretResponse(callUnary(DELETE_SECRET_METHOD, request));
        if (response.statusCode != 0) {
            throw new OperationFailureException(operr("key-tool delete secret failed: %s", response.statusMessage));
        }
    }

    private static ImportNkpResponse importNkp(String requestId, String contentBase64, String password, boolean persistRootKey) {
        String reqId = normalizeRequestId(requestId);
        byte[] request = buildImportNkpRequest(reqId, contentBase64, password, persistRootKey);
        ImportNkpResponse response = callImportNkp(request);
        if (response.statusCode != 0) {
            throw new OperationFailureException(operr("key-tool import nkp failed: %s", response.statusMessage));
        }
        return response;
    }

    private static byte[] buildHealthNkpRequest(String requestId, String name, String uuid) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CodedOutputStream cos = CodedOutputStream.newInstance(out);
            cos.writeString(1, requestId);
            byte[] nkp = buildNkpConfig(name, uuid);
            if (nkp.length > 0) {
                cos.writeByteArray(2, nkp);
            }
            cos.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to build nkp health request").withException(e.getMessage()));
        }
    }

    private static byte[] buildHealthKmipRequest(String requestId, KmipConfig kmipConfig) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CodedOutputStream cos = CodedOutputStream.newInstance(out);
            cos.writeString(1, requestId);
            byte[] kmip = buildKmipConfigFromObject(kmipConfig);
            if (kmip.length > 0) {
                cos.writeByteArray(2, kmip);
            }
            cos.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to build kmip health request").withException(e.getMessage()));
        }
    }

    private static byte[] buildRekeySecretRequest(String requestId, String secretRef, String name,
                                                  NkpConfig targetNkp, KmipConfig targetKmip,
                                                  NkpConfig originNkp, KmipConfig originKmip) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CodedOutputStream cos = CodedOutputStream.newInstance(out);
            cos.writeString(1, requestId);
            if (!StringUtils.isBlank(secretRef)) {
                cos.writeString(2, secretRef);
            }
            byte[] targetKmipBytes = buildKmipConfigFromObject(targetKmip);
            if (targetKmipBytes.length > 0) {
                cos.writeByteArray(3, targetKmipBytes);
            }
            byte[] targetNkpBytes = buildNkpConfigFromObject(targetNkp);
            if (targetNkpBytes.length > 0) {
                cos.writeByteArray(4, targetNkpBytes);
            }
            byte[] originNkpBytes = buildNkpConfigFromObject(originNkp);
            if (originNkpBytes.length > 0) {
                cos.writeByteArray(5, originNkpBytes);
            }
            byte[] originKmipBytes = buildKmipConfigFromObject(originKmip);
            if (originKmipBytes.length > 0) {
                cos.writeByteArray(6, originKmipBytes);
            }
            if (!StringUtils.isBlank(name)) {
                cos.writeString(7, name);
            }
            cos.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to build rekey secret request").withException(e.getMessage()));
        }
    }

    private static byte[] buildCreateSecretRequest(String requestId, String name, NkpConfig targetNkp, KmipConfig targetKmip) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CodedOutputStream cos = CodedOutputStream.newInstance(out);
            cos.writeString(1, requestId);
            byte[] targetKmipBytes = buildKmipConfigFromObject(targetKmip);
            if (targetKmipBytes.length > 0) {
                cos.writeByteArray(2, targetKmipBytes);
            }
            byte[] targetNkpBytes = buildNkpConfigFromObject(targetNkp);
            if (targetNkpBytes.length > 0) {
                cos.writeByteArray(3, targetNkpBytes);
            }
            if (!StringUtils.isBlank(name)) {
                cos.writeString(4, name);
            }
            cos.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to build create secret request").withException(e.getMessage()));
        }
    }

    private static byte[] buildGetSecretRequest(String requestId, String secretRef, NkpConfig targetNkp, KmipConfig targetKmip) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CodedOutputStream cos = CodedOutputStream.newInstance(out);
            cos.writeString(1, requestId);
            if (!StringUtils.isBlank(secretRef)) {
                cos.writeString(2, secretRef);
            }
            byte[] targetKmipBytes = buildKmipConfigFromObject(targetKmip);
            if (targetKmipBytes.length > 0) {
                cos.writeByteArray(3, targetKmipBytes);
            }
            byte[] targetNkpBytes = buildNkpConfigFromObject(targetNkp);
            if (targetNkpBytes.length > 0) {
                cos.writeByteArray(4, targetNkpBytes);
            }
            cos.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to build get secret request").withException(e.getMessage()));
        }
    }

    private static byte[] buildDeleteSecretRequest(String requestId, String secretRef, NkpConfig targetNkp, KmipConfig targetKmip) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CodedOutputStream cos = CodedOutputStream.newInstance(out);
            cos.writeString(1, requestId);
            if (!StringUtils.isBlank(secretRef)) {
                cos.writeString(2, secretRef);
            }
            byte[] targetKmipBytes = buildKmipConfigFromObject(targetKmip);
            if (targetKmipBytes.length > 0) {
                cos.writeByteArray(3, targetKmipBytes);
            }
            byte[] targetNkpBytes = buildNkpConfigFromObject(targetNkp);
            if (targetNkpBytes.length > 0) {
                cos.writeByteArray(4, targetNkpBytes);
            }
            cos.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to build delete secret request").withException(e.getMessage()));
        }
    }

    private static ImportNkpResponse importNkpWithStatus(String requestId, String contentBase64, String password, boolean persistRootKey) {
        String reqId = normalizeRequestId(requestId);
        byte[] request = buildImportNkpRequest(reqId, contentBase64, password, persistRootKey);
        return callImportNkp(request);
    }

    private static String normalizeRequestId(String requestId) {
        if (!StringUtils.isBlank(requestId)) {
            return requestId;
        }
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private static MethodDescriptor<byte[], byte[]> buildUnaryMethod(String methodName) {
        return buildUnaryMethod(SERVICE_NAME, methodName);
    }

    private static MethodDescriptor<byte[], byte[]> buildUnaryMethod(String serviceName, String methodName) {
        return MethodDescriptor.<byte[], byte[]>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
                .setRequestMarshaller(ByteArrayMarshaller.INSTANCE)
                .setResponseMarshaller(ByteArrayMarshaller.INSTANCE)
                .build();
    }

    private static PackNkpResponse callPackNkp(byte[] request) {
        return parsePackNkpResponse(callUnary(PACK_NKP_METHOD, request));
    }

    private static ImportNkpResponse callImportNkp(byte[] request) {
        return parseImportNkpResponse(callUnary(IMPORT_NKP_METHOD, request));
    }

    private static byte[] callUnary(MethodDescriptor<byte[], byte[]> method, byte[] request) {
        Integer port = KeyProviderGlobalConfig.KEY_TOOL_GRPC_PORT.value(Integer.class);
        if (port == null) {
            throw new OperationFailureException(operr("key-tool grpc port is empty"));
        }
        String address = String.format("127.0.0.1:%d", port);
        ManagedChannel channel = getOrCreateChannel(address);
        CallOptions options = CallOptions.DEFAULT.withDeadlineAfter(KEY_TOOL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        try {
            return ClientCalls.blockingUnaryCall(channel, method, options, request);
        } catch (StatusRuntimeException e) {
            if (isUnrecoverableChannelError(e.getStatus().getCode())) {
                invalidateChannel(address, channel);
            }
            throw e;
        }
    }

    private static boolean isUnrecoverableChannelError(Status.Code code) {
        return code == Status.Code.UNAVAILABLE
                || code == Status.Code.INTERNAL;
    }

    private static ManagedChannel getOrCreateChannel(String address) {
        ManagedChannel ch = CHANNEL_CACHE.get(address);
        if (ch != null && !ch.isShutdown() && !ch.isTerminated()) {
            return ch;
        }

        return CHANNEL_CACHE.compute(address, (key, existing) -> {
            if (existing != null && !existing.isShutdown() && !existing.isTerminated()) {
                return existing;
            }
            if (existing != null) {
                existing.shutdownNow();
            }
            return ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        });
    }

    private static void invalidateChannel(String address, ManagedChannel channel) {
        CHANNEL_CACHE.computeIfPresent(address, (key, existing) -> {
            if (existing != channel) {
                return existing;
            }
            existing.shutdownNow();
            return null;
        });
    }

    private static void shutdownAllChannels() {
        CHANNEL_CACHE.forEach((address, channel) -> {
            try {
                channel.shutdownNow();
            } catch (Throwable ignored) {
            }
        });
        CHANNEL_CACHE.clear();
    }

    private static byte[] buildNkpConfig(String name, String uuid) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CodedOutputStream cos = CodedOutputStream.newInstance(out);
            if (!StringUtils.isBlank(name)) {
                cos.writeString(1, name);
            }
            if (!StringUtils.isBlank(uuid)) {
                cos.writeString(2, uuid);
            }
            cos.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to build nkp config").withException(e.getMessage()));
        }
    }

    private static byte[] buildNkpConfigFromObject(NkpConfig cfg) {
        if (cfg == null) {
            return new byte[0];
        }
        return buildNkpConfig(cfg.name, cfg.uuid);
    }

    private static byte[] buildPackNkpRequest(String requestId, String name, String uuid, String password, byte[] metaData) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CodedOutputStream cos = CodedOutputStream.newInstance(out);
            cos.writeString(1, requestId);
            cos.writeByteArray(2, buildNkpConfig(name, uuid));
            if (password != null) {
                cos.writeString(3, password);
            }
            if (metaData != null && metaData.length > 0) {
                cos.writeByteArray(4, metaData);
            }
            cos.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to build pack nkp request").withException(e.getMessage()));
        }
    }

    private static byte[] buildImportNkpRequest(String requestId, String contentBase64, String password, boolean persistRootKey) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CodedOutputStream cos = CodedOutputStream.newInstance(out);
            cos.writeString(1, requestId);
            cos.writeString(2, contentBase64);
            if (password != null) {
                cos.writeString(3, password);
            }
            cos.writeBool(4, persistRootKey);
            cos.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to build import nkp request").withException(e.getMessage()));
        }
    }

    private static byte[] buildKmipConfig(String host, Integer port, String version,
                                          String username, String password,
                                          String clientCertPem, String clientKeyPem, String caCertPem) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CodedOutputStream cos = CodedOutputStream.newInstance(out);
            if (!StringUtils.isBlank(host)) {
                cos.writeString(1, host);
            }
            if (port != null) {
                cos.writeUInt32(2, port);
            }
            if (!StringUtils.isBlank(clientCertPem)) {
                cos.writeString(3, clientCertPem);
            }
            if (!StringUtils.isBlank(clientKeyPem)) {
                cos.writeString(4, clientKeyPem);
            }
            if (!StringUtils.isBlank(caCertPem)) {
                cos.writeString(5, caCertPem);
            }
            if (!StringUtils.isBlank(username)) {
                cos.writeString(6, username);
            }
            if (!StringUtils.isBlank(password)) {
                cos.writeString(7, password);
            }
            if (!StringUtils.isBlank(version)) {
                cos.writeString(8, version);
            }
            cos.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to build kmip config").withException(e.getMessage()));
        }
    }

    private static byte[] buildKmipConfigFromObject(KmipConfig cfg) {
        if (cfg == null) {
            return new byte[0];
        }
        return buildKmipConfig(cfg.host, cfg.port, cfg.version, cfg.username, cfg.password,
                cfg.clientCertPem, cfg.clientKeyPem, cfg.caCertPem);
    }

    private static PackNkpResponse parsePackNkpResponse(byte[] response) {
        PackNkpResponse parsed = new PackNkpResponse();
        try {
            CodedInputStream cis = CodedInputStream.newInstance(response);
            int tag;
            while ((tag = cis.readTag()) != 0) {
                int field = WireFormat.getTagFieldNumber(tag);
                switch (field) {
                    case 1:
                        parsed.statusCode = cis.readInt32();
                        break;
                    case 2:
                        parsed.statusMessage = cis.readString();
                        break;
                    case 3:
                        parsed.backupData = cis.readString();
                        break;
                    default:
                        cis.skipField(tag);
                        break;
                }
            }
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to parse pack nkp response").withException(e.getMessage()));
        }
        return parsed;
    }

    private static ImportNkpResponse parseImportNkpResponse(byte[] response) {
        ImportNkpResponse parsed = new ImportNkpResponse();
        try {
            CodedInputStream cis = CodedInputStream.newInstance(response);
            int tag;
            while ((tag = cis.readTag()) != 0) {
                int field = WireFormat.getTagFieldNumber(tag);
                switch (field) {
                    case 1:
                        parsed.statusCode = cis.readInt32();
                        break;
                    case 2:
                        parsed.statusMessage = cis.readString();
                        break;
                    case 3:
                        parsed.metaData = cis.readByteArray();
                        break;
                    default:
                        cis.skipField(tag);
                        break;
                }
            }
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to parse import nkp response").withException(e.getMessage()));
        }
        return parsed;
    }

    private static HealthResult parseHealthResponse(byte[] response) {
        HealthResult parsed = new HealthResult();
        try {
            CodedInputStream cis = CodedInputStream.newInstance(response);
            int tag;
            while ((tag = cis.readTag()) != 0) {
                int field = WireFormat.getTagFieldNumber(tag);
                switch (field) {
                    case 1:
                        parsed.statusCode = cis.readInt32();
                        break;
                    case 2:
                        parsed.statusMessage = cis.readString();
                        break;
                    case 3:
                        parsed.serverCertVerified = cis.readBool();
                        parsed.serverCertVerifiedPresent = true;
                        break;
                    case 4:
                        parsed.clientCertVerified = cis.readBool();
                        parsed.clientCertVerifiedPresent = true;
                        break;
                    case 5:
                        parsed.connected = cis.readBool();
                        parsed.connectedPresent = true;
                        break;
                    default:
                        cis.skipField(tag);
                        break;
                }
            }
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to parse health response").withException(e.getMessage()));
        }
        return parsed;
    }

    private static RekeySecretResponse parseRekeySecretResponse(byte[] response) {
        RekeySecretResponse parsed = new RekeySecretResponse();
        try {
            CodedInputStream cis = CodedInputStream.newInstance(response);
            int tag;
            while ((tag = cis.readTag()) != 0) {
                int field = WireFormat.getTagFieldNumber(tag);
                switch (field) {
                    case 1:
                        parsed.statusCode = cis.readInt32();
                        break;
                    case 2:
                        parsed.statusMessage = cis.readString();
                        break;
                    case 3:
                        parsed.newSecretRef = cis.readString();
                        break;
                    default:
                        cis.skipField(tag);
                        break;
                }
            }
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to parse rekey secret response").withException(e.getMessage()));
        }
        return parsed;
    }

    private static CreateSecretResponse parseCreateSecretResponse(byte[] response) {
        CreateSecretResponse parsed = new CreateSecretResponse();
        try {
            CodedInputStream cis = CodedInputStream.newInstance(response);
            int tag;
            while ((tag = cis.readTag()) != 0) {
                int field = WireFormat.getTagFieldNumber(tag);
                switch (field) {
                    case 1:
                        parsed.secretRef = cis.readString();
                        break;
                    case 2:
                        parsed.statusCode = cis.readInt32();
                        break;
                    case 3:
                        parsed.statusMessage = cis.readString();
                        break;
                    default:
                        cis.skipField(tag);
                        break;
                }
            }
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to parse create secret response").withException(e.getMessage()));
        }
        return parsed;
    }

    private static GetSecretResponse parseGetSecretResponse(byte[] response) {
        GetSecretResponse parsed = new GetSecretResponse();
        try {
            CodedInputStream cis = CodedInputStream.newInstance(response);
            int tag;
            while ((tag = cis.readTag()) != 0) {
                int field = WireFormat.getTagFieldNumber(tag);
                switch (field) {
                    case 1:
                        parsed.payload = cis.readByteArray();
                        break;
                    case 2:
                        parsed.statusCode = cis.readInt32();
                        break;
                    case 3:
                        parsed.statusMessage = cis.readString();
                        break;
                    default:
                        cis.skipField(tag);
                        break;
                }
            }
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to parse get secret response").withException(e.getMessage()));
        }
        return parsed;
    }

    private static DeleteSecretResponse parseDeleteSecretResponse(byte[] response) {
        DeleteSecretResponse parsed = new DeleteSecretResponse();
        try {
            CodedInputStream cis = CodedInputStream.newInstance(response);
            int tag;
            while ((tag = cis.readTag()) != 0) {
                int field = WireFormat.getTagFieldNumber(tag);
                switch (field) {
                    case 1:
                        parsed.statusCode = cis.readInt32();
                        break;
                    case 2:
                        parsed.statusMessage = cis.readString();
                        break;
                    default:
                        cis.skipField(tag);
                        break;
                }
            }
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to parse delete secret response").withException(e.getMessage()));
        }
        return parsed;
    }

    private static class PackNkpResponse {
        private int statusCode;
        private String statusMessage;
        private String backupData;
    }

    private static class ImportNkpResponse {
        private int statusCode;
        private String statusMessage;
        private byte[] metaData;
    }

    private static class RekeySecretResponse {
        private int statusCode;
        private String statusMessage;
        private String newSecretRef;
    }

    private static class CreateSecretResponse {
        private String secretRef;
        private int statusCode;
        private String statusMessage;
    }

    private static class GetSecretResponse {
        private byte[] payload;
        private int statusCode;
        private String statusMessage;
    }

    private static class DeleteSecretResponse {
        private int statusCode;
        private String statusMessage;
    }

    public static class ImportNkpResult {
        private final int statusCode;
        private final String statusMessage;
        private final byte[] metaData;

        public ImportNkpResult(int statusCode, String statusMessage, byte[] metaData) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
            this.metaData = metaData;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public byte[] getMetaData() {
            return metaData;
        }
    }

    public static class HealthResult {
        private int statusCode;
        private String statusMessage;
        private boolean serverCertVerified;
        private boolean clientCertVerified;
        private boolean connected;
        private boolean serverCertVerifiedPresent;
        private boolean clientCertVerifiedPresent;
        private boolean connectedPresent;

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public boolean isHealthy() {
            return statusCode == 0;
        }

        public boolean isServerCertVerified() {
            return serverCertVerified;
        }

        public boolean hasServerCertVerified() {
            return serverCertVerifiedPresent;
        }

        public boolean isClientCertVerified() {
            return clientCertVerified;
        }

        public boolean hasClientCertVerified() {
            return clientCertVerifiedPresent;
        }

        public boolean isConnected() {
            return connectedPresent ? connected : KeyProviderErrors.isKmipConnectedStatus(statusCode);
        }

        public boolean hasConnected() {
            return connectedPresent;
        }

        public String getError() {
            return statusMessage;
        }

        public static HealthResult fail(String message) {
            HealthResult result = new HealthResult();
            result.statusCode = -1;
            result.statusMessage = message;
            return result;
        }
    }

    private static class ByteArrayMarshaller implements MethodDescriptor.Marshaller<byte[]> {
        private static final ByteArrayMarshaller INSTANCE = new ByteArrayMarshaller();

        @Override
        public InputStream stream(byte[] value) {
            return new ByteArrayInputStream(value == null ? new byte[0] : value);
        }

        @Override
        public byte[] parse(InputStream stream) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = stream.read(buffer)) >= 0) {
                    if (read > 0) {
                        out.write(buffer, 0, read);
                    }
                }
                return out.toByteArray();
            } catch (Exception e) {
                throw new OperationFailureException(operr("failed to parse grpc response").withException(e.getMessage()));
            }
        }
    }
}
