package org.zstack.crypto.keyprovider;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.Platform;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO_;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KeyProviderVO_;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.keyprovider.KmsVO_;
import org.zstack.header.keyprovider.KmsIdentityVO;
import org.zstack.header.keyprovider.NkpVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Base64;
import java.util.List;

import static org.zstack.core.Platform.operr;

public class EncryptedResourceKeyManagerImpl implements EncryptedResourceKeyManager {
    private static final CLogger logger = Utils.getLogger(EncryptedResourceKeyManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void getOrCreateKey(GetOrCreateResourceKeyContext ctx,
                               ReturnValueCompletion<ResourceKeyResult> completion) {
        try {
            validateContext(ctx);
            ResourceKeyResult result = doGetOrCreateKey(ctx);
            completion.success(result);
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        } catch (Exception e) {
            completion.fail(operr("failed to get or create resource key for %s[uuid:%s]",
                    ctx.getResourceType(), ctx.getResourceUuid())
                    .withException(e.getMessage()));
        }
    }

    @Override
    public ResourceKeyResult getKey(GetOrCreateResourceKeyContext ctx) {
        try {
            validateContext(ctx);
            return doGetKey(ctx);
        } catch (OperationFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to get existing resource key for %s[uuid:%s]",
                    ctx.getResourceType(), ctx.getResourceUuid())
                    .withException(e.getMessage()));
        }
    }

    private ResourceKeyResult doGetKey(GetOrCreateResourceKeyContext ctx) {
        KeyProviderVO provider = resolveProvider(ctx);

        NkpConfig nkpConfig = null;
        KmipConfig kmipConfig = null;
        if (provider.getType() == KeyProviderType.NKP) {
            NkpVO nkp = dbf.findByUuid(provider.getUuid(), NkpVO.class);
            if (nkp == null) {
                throw new OperationFailureException(operr("cannot find nkp[uuid:%s]", provider.getUuid()));
            }
            nkpConfig = new NkpConfig(nkp.getName(), nkp.getUuid());
        } else if (provider.getType() == KeyProviderType.KMS) {
            KmsVO kms = Q.New(KmsVO.class).eq(KmsVO_.uuid, provider.getUuid()).find();
            if (kms == null) {
                throw new OperationFailureException(operr("cannot find kms[uuid:%s]", provider.getUuid()));
            }
            kmipConfig = buildKmipConfig(kms);
        } else {
            throw new OperationFailureException(operr("unsupported key provider type[%s]", provider.getType()));
        }

        EncryptedResourceKeyRefVO ref = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, ctx.getResourceUuid())
                .eq(EncryptedResourceKeyRefVO_.resourceType, ctx.getResourceType())
                .find();
        if (ref == null) {
            throw new OperationFailureException(operr(
                    "no encrypted resource key ref for %s[uuid:%s]; hot migration requires an existing key binding",
                    ctx.getResourceType(), ctx.getResourceUuid()));
        }
        ref = normalizeExistingRef(ref, provider, ctx);
        if (StringUtils.isBlank(ref.getKekRef())) {
            throw new OperationFailureException(operr(
                    "encrypted resource key ref for %s[uuid:%s] has no secret reference; hot migration will not create one",
                    ctx.getResourceType(), ctx.getResourceUuid()));
        }

        validateExistingProviderBinding(ref, provider, ctx);

        String requestId = Platform.getUuid();
        String existingSecretRef = ref.getKekRef();
        logger.info(String.format("loading existing resource key for %s[uuid:%s] (get-only, hot migrate)",
                ctx.getResourceType(), ctx.getResourceUuid()));
        byte[] plaintextDek = KeyToolGrpcClient.getSecret(requestId, existingSecretRef, nkpConfig, kmipConfig);
        refreshProviderBinding(ref, provider, ctx);

        ResourceKeyResult result = new ResourceKeyResult();
        result.setResourceUuid(ctx.getResourceUuid());
        result.setResourceType(ctx.getResourceType());
        result.setKeyProviderUuid(provider.getUuid());
        result.setKeyProviderName(provider.getName());
        result.setKeyVersion(ref.getKeyVersion());
        result.setDekBase64(Base64.getEncoder().encodeToString(plaintextDek));
        result.setSecretRef(existingSecretRef);
        result.setCreatedNewKey(false);
        return result;
    }

    private ResourceKeyResult doGetOrCreateKey(GetOrCreateResourceKeyContext ctx) {
        KeyProviderVO provider = resolveProvider(ctx);

        NkpConfig nkpConfig = null;
        KmipConfig kmipConfig = null;
        if (provider.getType() == KeyProviderType.NKP) {
            NkpVO nkp = dbf.findByUuid(provider.getUuid(), NkpVO.class);
            if (nkp == null) {
                throw new OperationFailureException(operr("cannot find nkp[uuid:%s]", provider.getUuid()));
            }
            nkpConfig = new NkpConfig(nkp.getName(), nkp.getUuid());
        } else if (provider.getType() == KeyProviderType.KMS) {
            KmsVO kms = Q.New(KmsVO.class).eq(KmsVO_.uuid, provider.getUuid()).find();
            if (kms == null) {
                throw new OperationFailureException(operr("cannot find kms[uuid:%s]", provider.getUuid()));
            }
            kmipConfig = buildKmipConfig(kms);
        } else {
            throw new OperationFailureException(operr("unsupported key provider type[%s]", provider.getType()));
        }

        EncryptedResourceKeyRefVO ref = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, ctx.getResourceUuid())
                .eq(EncryptedResourceKeyRefVO_.resourceType, ctx.getResourceType())
                .find();
        ref = normalizeExistingRef(ref, provider, ctx);

        String requestId = Platform.getUuid();
        byte[] plaintextDek;
        String existingSecretRef = null;
        String effectiveSecretRef = null;
        if (ref != null) {
            existingSecretRef = ref.getKekRef();
        }

        validateExistingProviderBinding(ref, provider, ctx);

        if (StringUtils.isNotBlank(existingSecretRef)) {
            logger.info(String.format("found existing resource key for %s[uuid:%s], retrieving plaintext DEK",
                    ctx.getResourceType(), ctx.getResourceUuid()));
            plaintextDek = KeyToolGrpcClient.getSecret(requestId, existingSecretRef, nkpConfig, kmipConfig);
            effectiveSecretRef = existingSecretRef;
            refreshProviderBinding(ref, provider, ctx);
        } else {
            logger.info(String.format("creating new resource key for %s[uuid:%s] with provider[uuid:%s]",
                    ctx.getResourceType(), ctx.getResourceUuid(), provider.getUuid()));

            String secretName = String.format("%s-%s", ctx.getResourceType(), ctx.getResourceUuid());
            String secretRef = KeyToolGrpcClient.createSecret(requestId, secretName, nkpConfig, kmipConfig);
            ref = persistOrUpdateRef(ref, ctx, provider, secretRef);
            plaintextDek = KeyToolGrpcClient.getSecret(requestId, secretRef, nkpConfig, kmipConfig);
            effectiveSecretRef = secretRef;
        }

        ResourceKeyResult result = new ResourceKeyResult();
        result.setResourceUuid(ctx.getResourceUuid());
        result.setResourceType(ctx.getResourceType());
        result.setKeyProviderUuid(provider.getUuid());
        result.setKeyProviderName(provider.getName());
        result.setKeyVersion(ref != null ? ref.getKeyVersion() : 1);
        result.setDekBase64(Base64.getEncoder().encodeToString(plaintextDek));
        result.setSecretRef(effectiveSecretRef);
        result.setCreatedNewKey(StringUtils.isBlank(existingSecretRef));
        return result;
    }

    @Override
    public void rollbackCreatedKey(ResourceKeyResult result, Completion completion) {
        if (result == null || !result.isCreatedNewKey()) {
            completion.success();
            return;
        }

        try {
            if (StringUtils.isBlank(result.getSecretRef())) {
                EncryptedResourceKeyRefVO ref = Q.New(EncryptedResourceKeyRefVO.class)
                        .eq(EncryptedResourceKeyRefVO_.resourceUuid, result.getResourceUuid())
                        .eq(EncryptedResourceKeyRefVO_.resourceType, result.getResourceType())
                        .find();
                if (ref != null) {
                    result.setSecretRef(ref.getKekRef());
                }
            }

            KeyProviderVO provider = Q.New(KeyProviderVO.class)
                    .eq(KeyProviderVO_.uuid, result.getKeyProviderUuid())
                    .find();
            if (provider == null) {
                SQL.New(EncryptedResourceKeyRefVO.class)
                    .eq(EncryptedResourceKeyRefVO_.resourceUuid, result.getResourceUuid())
                    .eq(EncryptedResourceKeyRefVO_.resourceType, result.getResourceType())
                    .delete();
                completion.success();
                return;
            }

            // key-tool deleteSecret is supported for KMIP-backed secrets only; NKP does not support delete key.
            if (provider.getType() == KeyProviderType.KMS && StringUtils.isNotBlank(result.getSecretRef())) {
                KmsVO kms = Q.New(KmsVO.class).eq(KmsVO_.uuid, provider.getUuid()).find();
                if (kms != null) {
                    KmipConfig kmipConfig = buildKmipConfig(kms);
                    try {
                        KeyToolGrpcClient.deleteSecret(Platform.getUuid(),
                                result.getSecretRef(), null, kmipConfig);
                    } catch (Exception e) {
                        String details = String.format("failed to delete key-tool secret during rollback for %s[uuid:%s], secretRef:%s, %s",
                                result.getResourceType(), result.getResourceUuid(), result.getSecretRef(), e.getMessage());
                        logger.warn(details);
                        completion.fail(operr(details));
                        return;
                    }
                }
            } else if (provider.getType() == KeyProviderType.NKP) {
                logger.debug(String.format(
                        "rollback for %s[uuid:%s]: skip key-tool deleteSecret (NKP does not support delete key); removing local ref only",
                        result.getResourceType(), result.getResourceUuid()));
            }

            SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, result.getResourceUuid())
                .eq(EncryptedResourceKeyRefVO_.resourceType, result.getResourceType())
                .delete();
            completion.success();
        } catch (Exception e) {
            completion.fail(operr("failed to rollback resource key for %s[uuid:%s]: %s",
                    result.getResourceType(), result.getResourceUuid(), e.getMessage()));
        }
    }

    private void validateContext(GetOrCreateResourceKeyContext ctx) {
        if (ctx == null) {
            throw new OperationFailureException(operr("getOrCreate resource key context is null"));
        }
        if (StringUtils.isBlank(ctx.getResourceType())) {
            throw new OperationFailureException(operr("resourceType is required"));
        }
        if (StringUtils.isBlank(ctx.getResourceUuid())) {
            throw new OperationFailureException(operr("resourceUuid is required"));
        }
        if (StringUtils.isBlank(ctx.getKeyProviderUuid()) && StringUtils.isBlank(ctx.getKeyProviderName())) {
            throw new OperationFailureException(operr(
                    "key provider is not found for %s[uuid:%s]",
                    ctx.getResourceType(), ctx.getResourceUuid()));
        }
    }

    private KeyProviderVO resolveProvider(GetOrCreateResourceKeyContext ctx) {
        KeyProviderVO provider = null;
        if (StringUtils.isNotBlank(ctx.getKeyProviderUuid())) {
            provider = Q.New(KeyProviderVO.class)
                    .eq(KeyProviderVO_.uuid, ctx.getKeyProviderUuid())
                    .find();
        }

        if (provider == null && StringUtils.isNotBlank(ctx.getKeyProviderName())) {
            List<KeyProviderVO> providers = Q.New(KeyProviderVO.class)
                    .eq(KeyProviderVO_.name, ctx.getKeyProviderName())
                    .list();
            if (providers.size() > 1) {
                throw new OperationFailureException(operr("multiple key providers found by name[%s]", ctx.getKeyProviderName()));
            }
            if (!providers.isEmpty()) {
                provider = providers.get(0);
                logger.debug(String.format("fallback to key provider lookup by name for %s[uuid:%s], requested providerUuid:%s, providerName:%s",
                        ctx.getResourceType(), ctx.getResourceUuid(), ctx.getKeyProviderUuid(), ctx.getKeyProviderName()));
            }
        }

        if (provider == null) {
            if (StringUtils.isNotBlank(ctx.getKeyProviderName())) {
                throw new OperationFailureException(operr(
                        "key provider [name:%s] does not exist for %s[uuid:%s]; "
                                + "verify the name or add the key provider first",
                        ctx.getKeyProviderName(), ctx.getResourceType(), ctx.getResourceUuid()));
            }
            throw new OperationFailureException(operr(
                    "key provider [uuid:%s] does not exist for %s[uuid:%s]; verify the UUID or add the key provider first",
                    ctx.getKeyProviderUuid(), ctx.getResourceType(), ctx.getResourceUuid()));
        }

        return provider;
    }

    private KmipConfig buildKmipConfig(KmsVO kms) {
        KmsIdentityVO identity = kms.getActiveIdentity();
        return KmipConfig.fromKms(kms, identity);
    }

    private EncryptedResourceKeyRefVO normalizeExistingRef(EncryptedResourceKeyRefVO ref,
                                                           KeyProviderVO provider,
                                                           GetOrCreateResourceKeyContext ctx) {
        if (ref == null) {
            return ref;
        }

        boolean needsKeyVersionNormalize = ref.getKeyVersion() == null;
        boolean needsSecretRefNormalize = StringUtils.isBlank(ref.getKekRef())
                && StringUtils.isNotBlank(ref.getWrappedDek());

        if (!needsKeyVersionNormalize && !needsSecretRefNormalize) {
            return ref;
        }

        if (needsSecretRefNormalize && provider.getType() != KeyProviderType.NKP) {
            throw new OperationFailureException(operr(
                    "resource key ref for %s[uuid:%s] has legacy wrappedDek but provider type[%s] cannot be auto-converted",
                    ctx.getResourceType(), ctx.getResourceUuid(), provider.getType()));
        }

        if (needsKeyVersionNormalize) {
            logger.info(String.format("normalize missing keyVersion to 1 for %s[uuid:%s]",
                    ctx.getResourceType(), ctx.getResourceUuid()));
        }
        if (needsSecretRefNormalize) {
            logger.info(String.format("normalize legacy wrappedDek to secretRef for %s[uuid:%s]",
                    ctx.getResourceType(), ctx.getResourceUuid()));
        }

        String secretRef = null;
        if (needsSecretRefNormalize) {
            secretRef = ref.getWrappedDek().startsWith("nkp://")
                    ? ref.getWrappedDek()
                    : "nkp://secret/" + ref.getWrappedDek();
        }

        if (needsKeyVersionNormalize && needsSecretRefNormalize) {
            SQL.New(EncryptedResourceKeyRefVO.class)
                    .eq(EncryptedResourceKeyRefVO_.resourceUuid, ctx.getResourceUuid())
                    .eq(EncryptedResourceKeyRefVO_.resourceType, ctx.getResourceType())
                    .set(EncryptedResourceKeyRefVO_.keyVersion, 1)
                    .set(EncryptedResourceKeyRefVO_.kekRef, secretRef)
                    .set(EncryptedResourceKeyRefVO_.wrappedDek, "")
                    .update();
            ref.setKeyVersion(1);
            ref.setKekRef(secretRef);
            ref.setWrappedDek("");
        } else if (needsKeyVersionNormalize) {
            SQL.New(EncryptedResourceKeyRefVO.class)
                    .eq(EncryptedResourceKeyRefVO_.resourceUuid, ctx.getResourceUuid())
                    .eq(EncryptedResourceKeyRefVO_.resourceType, ctx.getResourceType())
                    .set(EncryptedResourceKeyRefVO_.keyVersion, 1)
                    .update();
            ref.setKeyVersion(1);
        } else {
            SQL.New(EncryptedResourceKeyRefVO.class)
                    .eq(EncryptedResourceKeyRefVO_.resourceUuid, ctx.getResourceUuid())
                    .eq(EncryptedResourceKeyRefVO_.resourceType, ctx.getResourceType())
                    .set(EncryptedResourceKeyRefVO_.kekRef, secretRef)
                    .set(EncryptedResourceKeyRefVO_.wrappedDek, "")
                    .update();
            ref.setKekRef(secretRef);
            ref.setWrappedDek("");
        }

        return ref;
    }

    private void validateExistingProviderBinding(EncryptedResourceKeyRefVO ref,
                                                 KeyProviderVO provider,
                                                 GetOrCreateResourceKeyContext ctx) {
        if (ref == null) {
            return;
        }

        if (StringUtils.isNotBlank(ref.getProviderName())
                && !StringUtils.equals(ref.getProviderName(), provider.getName())) {
            throw new OperationFailureException(operr(
                    "resource key for %s[uuid:%s] is already bound to another key provider[name:%s], requested provider[name:%s]",
                    ctx.getResourceType(), ctx.getResourceUuid(), ref.getProviderName(), provider.getName()));
        }
    }

    private void refreshProviderBinding(EncryptedResourceKeyRefVO ref,
                                        KeyProviderVO provider,
                                        GetOrCreateResourceKeyContext ctx) {
        if (ref == null) {
            return;
        }

        if (StringUtils.equals(ref.getProviderUuid(), provider.getUuid())
                && StringUtils.equals(ref.getProviderName(), provider.getName())) {
            return;
        }

        SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, ctx.getResourceUuid())
                .eq(EncryptedResourceKeyRefVO_.resourceType, ctx.getResourceType())
                .set(EncryptedResourceKeyRefVO_.providerUuid, provider.getUuid())
                .set(EncryptedResourceKeyRefVO_.providerName, provider.getName())
                .update();
        ref.setProviderUuid(provider.getUuid());
        ref.setProviderName(provider.getName());
    }

    private EncryptedResourceKeyRefVO persistOrUpdateRef(EncryptedResourceKeyRefVO ref, GetOrCreateResourceKeyContext ctx,
                                                         KeyProviderVO provider, String secretRef) {
        if (ref != null) {
            SQL.New(EncryptedResourceKeyRefVO.class)
                    .eq(EncryptedResourceKeyRefVO_.resourceUuid, ctx.getResourceUuid())
                    .eq(EncryptedResourceKeyRefVO_.resourceType, ctx.getResourceType())
                    .set(EncryptedResourceKeyRefVO_.providerUuid, provider.getUuid())
                    .set(EncryptedResourceKeyRefVO_.providerName, provider.getName())
                    .set(EncryptedResourceKeyRefVO_.kekRef, secretRef)
                    .set(EncryptedResourceKeyRefVO_.wrappedDek, "")
                    .set(EncryptedResourceKeyRefVO_.keyVersion, 1)
                    .update();
            ref.setProviderUuid(provider.getUuid());
            ref.setProviderName(provider.getName());
            ref.setKekRef(secretRef);
            ref.setWrappedDek("");
            ref.setKeyVersion(1);
            return ref;
        }

        ref = new EncryptedResourceKeyRefVO();
        ref.setResourceType(ctx.getResourceType());
        ref.setResourceUuid(ctx.getResourceUuid());
        ref.setProviderUuid(provider.getUuid());
        ref.setProviderName(provider.getName());
        ref.setKekRef(secretRef);
        ref.setWrappedDek("");
        ref.setKeyVersion(1);
        dbf.persist(ref);
        return ref;
    }
}
