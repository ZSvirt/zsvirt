package org.zstack.kvm.tpm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend;
import org.zstack.core.db.Q;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.kvm.vmfiles.AbstractVmHostBackupFileBase;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend.BackupEncryptedResourceKeyContext;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class TpmStateVmHostBackupFileBase extends AbstractVmHostBackupFileBase {
    private static final CLogger logger = Utils.getLogger(TpmStateVmHostBackupFileBase.class);

    @Autowired
    private TpmEncryptedResourceKeyBackend resourceKeyBackend;

    public TpmStateVmHostBackupFileBase(VmHostBackupFileVO self) {
        super(self);
    }

    @Override
    public VmHostFileType type() {
        return VmHostFileType.TpmState;
    }

    @Override
    public void afterBackup(VmHostBackupFileVO from) {
        String keyProviderName = KVMSystemTags.TPM_KEY_PROVIDER_NAME
                .getTokenByResourceUuid(from.getUuid(), KVMSystemTags.TPM_KEY_PROVIDER_NAME_TOKEN);
        if (keyProviderName == null) {
            logger.debug(String.format("no tpm key provider name system tag found on source VmHostBackupFileVO[uuid:%s], skip copying",
                    from.getUuid()));
            return;
        }

        createKeyProviderNameTag(keyProviderName);
        logger.debug(String.format("copied tpm key provider name[%s] from VmHostBackupFileVO[uuid:%s] to VmHostBackupFileVO[uuid:%s]",
                keyProviderName, from.getUuid(), self.getUuid()));
    }

    @Override
    public void afterBackup(VmHostFileVO from) {
        String tpmUuid = Q.New(TpmVO.class)
                .select(TpmVO_.uuid)
                .eq(TpmVO_.vmInstanceUuid, from.getVmInstanceUuid())
                .findValue();
        if (tpmUuid == null) {
            logger.debug(String.format("no TpmVO found for vm[uuid:%s], skip creating key provider name tag on VmHostBackupFileVO[uuid:%s]",
                    from.getVmInstanceUuid(), self.getUuid()));
            return;
        }

        String keyProviderName = resourceKeyBackend.findKeyProviderNameByTpm(tpmUuid);
        if (keyProviderName == null) {
            logger.debug(String.format("no key provider name found for tpm[uuid:%s] of vm[uuid:%s], skip creating tag on VmHostBackupFileVO[uuid:%s]",
                    tpmUuid, from.getVmInstanceUuid(), self.getUuid()));
            return;
        }

        createKeyProviderNameTag(keyProviderName);
        logger.debug(String.format("created tpm key provider name[%s] tag on VmHostBackupFileVO[uuid:%s] from tpm[uuid:%s] of vm[uuid:%s]",
                keyProviderName, self.getUuid(), tpmUuid, from.getVmInstanceUuid()));

        final BackupEncryptedResourceKeyContext context = new BackupEncryptedResourceKeyContext();
        context.srcResourceUuid = tpmUuid;
        context.dstResourceUuid = self.getUuid();
        resourceKeyBackend.backupEncryptedResourceKey(context);
    }

    @SuppressWarnings("unchecked")
    private void createKeyProviderNameTag(String keyProviderName) {
        SystemTagCreator creator = KVMSystemTags.TPM_KEY_PROVIDER_NAME.newSystemTagCreator(self.getUuid());
        creator.setTagByTokens(map(e(KVMSystemTags.TPM_KEY_PROVIDER_NAME_TOKEN, keyProviderName)));
        creator.inherent = true;
        creator.recreate = true;
        creator.create();
    }

    @Override
    public void clean() {
        logger.debug(String.format("clean TpmState VmBackupFileVO[uuid=%s]", self.getUuid()));
        resourceKeyBackend.cleanEncryptedResourceKey(self.getUuid());
        super.clean();
    }
}
