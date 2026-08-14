package org.zstack.crypto.keyprovider;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.core.Completion;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO_;
import org.zstack.header.keyprovider.KeyProviderGlobalConfig;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KeyProviderVO_;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Objects;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.keyprovider.KeyProviderErrors.TPM_ALREADY_ATTACHED_KEY_PROVIDER;

public class KeyProviderResourceKeyBackend implements TpmEncryptedResourceKeyBackend {
    private static final CLogger logger = Utils.getLogger(KeyProviderResourceKeyBackend.class);

    @Autowired
    private DatabaseFacade databaseFacade;
    @Autowired
    private TimeHelper timeHelper;

    @Override
    public void attachKeyProviderToTpm(String tpmUuid, String keyProviderUuid) {
        final String existKeyProviderUuid = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .select(EncryptedResourceKeyRefVO_.providerUuid)
                .findValue();
        if (Objects.equals(existKeyProviderUuid, keyProviderUuid)) {
            logger.debug(String.format("The key provider[uuid:%s] is already attached to tpm[uuid:%s], skip attach/reset DEK",
                    keyProviderUuid, tpmUuid));
            return;
        } else if (existKeyProviderUuid != null) {
            throw err(TPM_ALREADY_ATTACHED_KEY_PROVIDER,
                    "The key provider[uuid:%s] is already attached to tpm[uuid:%s]", existKeyProviderUuid, tpmUuid)
                    .withOpaque("exists.key.provider.uuid", existKeyProviderUuid)
                    .withOpaque("tpm.uuid", tpmUuid)
                    .toException();
        }

        final KeyProviderVO provider = Q.New(KeyProviderVO.class)
                .eq(KeyProviderVO_.uuid, keyProviderUuid)
                .find();
        if (provider == null) {
            throw operr("KeyProvider[uuid:%s] not found", keyProviderUuid).toException();
        }

        EncryptedResourceKeyRefVO ref = new EncryptedResourceKeyRefVO();
        ref.setResourceType(TpmVO.class.getSimpleName());
        ref.setResourceUuid(tpmUuid);
        ref.setProviderUuid(keyProviderUuid);
        ref.setProviderName(provider.getName());
        ref.setWrappedDek("");
        databaseFacade.persist(ref);
    }

    @Override
    public void detachKeyProviderFromTpm(String tpmUuid) {
        SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .delete();
    }

    @Override
    public String findKeyProviderUuidByTpm(String tpmUuid) {
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .select(EncryptedResourceKeyRefVO_.providerUuid)
                .findValue();
    }

    @Override
    public String findKeyProviderUuidByName(String providerName) {
        return Q.New(KeyProviderVO.class)
                .eq(KeyProviderVO_.name, providerName)
                .select(KeyProviderVO_.uuid)
                .findValue();
    }

    @Override
    public String findKeyProviderNameByTpm(String tpmUuid) {
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .select(EncryptedResourceKeyRefVO_.providerName)
                .findValue();
    }

    @Override
    public Integer findKeyVersionByTpm(String tpmUuid) {
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .select(EncryptedResourceKeyRefVO_.keyVersion)
                .findValue();
    }

    @Override
    public String defaultKeyProviderUuid() {
        String currentDefault = KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.value();
        if (KeyProviderGlobalConfig.NONE.equals(currentDefault)) {
            return null;
        }
        return currentDefault;
    }

    public int applyKeyProviderWithKek(String tpmUuid, String providerUuid) {
        String providerName = Q.New(KeyProviderVO.class)
                .eq(KeyProviderVO_.uuid, providerUuid)
                .select(KeyProviderVO_.name)
                .findValue();
        if (StringUtils.isBlank(providerName)) {
            logger.warn(String.format(
                    "skip applyKeyProviderWithKek: provider not found[uuid:%s], tpmUuid:%s",
                    providerUuid, tpmUuid));
            return 0;
        }

        return SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, TpmVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .isNull(EncryptedResourceKeyRefVO_.providerUuid)
                .notNull(EncryptedResourceKeyRefVO_.kekRef)
                .set(EncryptedResourceKeyRefVO_.providerUuid, providerUuid)
                .set(EncryptedResourceKeyRefVO_.providerName, providerName)
                .set(EncryptedResourceKeyRefVO_.lastOpDate, null)
                .update();
    }

    public boolean checkTpmKeyProviderAttached(String tpmUuid) {
        Long count = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, TpmVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .count();
        return count != null && count.longValue() > 0;
    }

    @Override
    public void cloneEncryptedResourceKey(CloneEncryptedResourceKeyContext context, Completion completion) {
        EncryptedResourceKeyRefVO src = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, context.srcTpmUuid)
                .find();
        if (src == null) {
            logger.debug(String.format("No encrypted resource key found for tpm[uuid:%s], skip clone to tpm[uuid:%s]",
                    context.srcTpmUuid, context.dstTpmUuid));
            completion.success();
            return;
        }

        if (context.resetTpm) {
            logger.info(String.format("reset encrypted resource key for TPM[uuid=%s] from source TPM[uuid=%s]",
                    context.dstTpmUuid, context.srcTpmUuid));
            String defaultProviderUuid = defaultKeyProviderUuid();
            String defaultProviderName = Q.New(KeyProviderVO.class)
                    .eq(KeyProviderVO_.uuid, defaultProviderUuid)
                    .select(KeyProviderVO_.name)
                    .findValue();
            persistOrUpdatePlaceholderRef(context.dstTpmUuid, defaultProviderUuid, defaultProviderName);
            logger.info(String.format(
                    "clone-reset TPM target provider selected, source[tpmUuid:%s], " +
                            "destination[tpmUuid:%s,providerUuid:%s]",
                    context.srcTpmUuid,
                    context.dstTpmUuid, defaultProviderUuid));
        } else {
            logger.info(String.format("clone KMS resource key: TPM[uuid=%s] -> TPM[uuid=%s]",
                    context.srcTpmUuid, context.dstTpmUuid));
            persistOrUpdateClonedRef(context.dstTpmUuid, src);
        }

        completion.success();
    }

    private void persistOrUpdatePlaceholderRef(String dstTpmUuid, String providerUuid, String providerName) {
        EncryptedResourceKeyRefVO existing = findRefByTpmUuid(dstTpmUuid);
        if (existing != null) {
            SQL.New(EncryptedResourceKeyRefVO.class)
                    .eq(EncryptedResourceKeyRefVO_.resourceUuid, dstTpmUuid)
                    .eq(EncryptedResourceKeyRefVO_.resourceType, TpmVO.class.getSimpleName())
                    .set(EncryptedResourceKeyRefVO_.providerUuid, providerUuid)
                    .set(EncryptedResourceKeyRefVO_.providerName, providerName)
                    .set(EncryptedResourceKeyRefVO_.keyVersion, null)
                    .set(EncryptedResourceKeyRefVO_.kekRef, null)
                    .set(EncryptedResourceKeyRefVO_.wrappedDek, "")
                    .set(EncryptedResourceKeyRefVO_.algorithm, null)
                    .update();
            return;
        }

        EncryptedResourceKeyRefVO ref = new EncryptedResourceKeyRefVO();
        ref.setResourceType(TpmVO.class.getSimpleName());
        ref.setResourceUuid(dstTpmUuid);
        ref.setProviderUuid(providerUuid);
        ref.setProviderName(providerName);
        ref.setWrappedDek("");
        databaseFacade.persist(ref);
    }

    private void persistOrUpdateClonedRef(String dstTpmUuid, EncryptedResourceKeyRefVO src) {
        EncryptedResourceKeyRefVO existing = findRefByTpmUuid(dstTpmUuid);
        if (existing != null) {
            SQL.New(EncryptedResourceKeyRefVO.class)
                    .eq(EncryptedResourceKeyRefVO_.resourceUuid, dstTpmUuid)
                    .eq(EncryptedResourceKeyRefVO_.resourceType, TpmVO.class.getSimpleName())
                    .set(EncryptedResourceKeyRefVO_.providerUuid, src.getProviderUuid())
                    .set(EncryptedResourceKeyRefVO_.providerName, src.getProviderName())
                    .set(EncryptedResourceKeyRefVO_.keyVersion, src.getKeyVersion())
                    .set(EncryptedResourceKeyRefVO_.kekRef, src.getKekRef())
                    .set(EncryptedResourceKeyRefVO_.wrappedDek, src.getWrappedDek())
                    .set(EncryptedResourceKeyRefVO_.algorithm, src.getAlgorithm())
                    .update();
            return;
        }

        EncryptedResourceKeyRefVO ref = new EncryptedResourceKeyRefVO();
        ref.setResourceType(TpmVO.class.getSimpleName());
        ref.setResourceUuid(dstTpmUuid);
        ref.setProviderUuid(src.getProviderUuid());
        ref.setProviderName(src.getProviderName());
        ref.setKeyVersion(src.getKeyVersion());
        ref.setKekRef(src.getKekRef());
        ref.setWrappedDek(src.getWrappedDek());
        ref.setAlgorithm(src.getAlgorithm());
        databaseFacade.persist(ref);
    }

    private EncryptedResourceKeyRefVO findRefByTpmUuid(String tpmUuid) {
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .eq(EncryptedResourceKeyRefVO_.resourceType, TpmVO.class.getSimpleName())
                .find();
    }

    @Override
    public void backupEncryptedResourceKey(BackupEncryptedResourceKeyContent context) {
        EncryptedResourceKeyRefVO src = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, context.srcTpmUuid)
                .find();
        if (src == null) {
            logger.debug(String.format("No encrypted resource key found for tpm[uuid:%s], skip backup to VmHostBackupFile[uuid:%s]",
                    context.srcTpmUuid, context.dstVmHostBackupFileUuid));
            return;
        }

        EncryptedResourceKeyRefVO backup = new EncryptedResourceKeyRefVO();
        backup.setResourceType(VmHostBackupFileVO.class.getSimpleName());
        backup.setResourceUuid(context.dstVmHostBackupFileUuid);
        backup.setProviderUuid(src.getProviderUuid());
        backup.setProviderName(src.getProviderName());
        backup.setKeyVersion(src.getKeyVersion());
        backup.setKekRef(src.getKekRef());
        backup.setWrappedDek(src.getWrappedDek());
        backup.setAlgorithm(src.getAlgorithm());
        backup.setCreateDate(timeHelper.getCurrentTimestamp());
        backup.setLastOpDate(backup.getCreateDate());
        databaseFacade.persist(backup);
    }

    @Override
    public void cleanEncryptedResourceKey(String vmHostBackupFileUuid) {
        SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, VmHostBackupFileVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, vmHostBackupFileUuid)
                .delete();
    }
}
