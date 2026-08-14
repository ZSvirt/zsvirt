package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.storage.backup.BackupMode;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.volume.backup.APICreateVmBackupMsg;
import org.zstack.header.storage.volume.backup.APICreateVolumeBackupMsg;
import org.zstack.header.storage.volume.backup.CreateVmBackupMsg;
import org.zstack.header.storage.volume.backup.CreateVolumeBackupMsg;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.identity.QuotaUtil;
import org.zstack.identity.ResourceHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Qi Le on 2020/7/3
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeBackupQuotaOperator implements Quota.QuotaOperator {

    @Autowired
    DatabaseFacade dbf;

    @Override
    public void checkQuota(APIMessage msg, Map<String, Quota.QuotaPair> pairs) {
        AccountType type = new QuotaUtil().getAccountType(msg.getSession().getAccountUuid());
        if (type != AccountType.SystemAdmin) {
            if (msg instanceof APICreateVmBackupMsg) {
                check((APICreateVmBackupMsg) msg, pairs);
            } else if (msg instanceof APICreateVolumeBackupMsg) {
                check((APICreateVolumeBackupMsg) msg, pairs);
            } else if (msg instanceof APIChangeResourceOwnerMsg) {
                check((APIChangeResourceOwnerMsg) msg, pairs);
            }
        } else {
            if (msg instanceof APIChangeResourceOwnerMsg) {
                check((APIChangeResourceOwnerMsg) msg, pairs);
            }
        }
    }

    @Override
    public void checkQuota(NeedQuotaCheckMessage msg, Map<String, Quota.QuotaPair> pairs) {
        AccountType type = new QuotaUtil().getAccountType(msg.getAccountUuid());
        if (type != AccountType.SystemAdmin) {
            if (msg instanceof CreateVmBackupMsg) {
                check((CreateVmBackupMsg) msg, pairs);
            } else if (msg instanceof CreateVolumeBackupMsg) {
                check((CreateVolumeBackupMsg) msg, pairs);
            }
        }
    }

    @Transactional(readOnly = true)
    protected void check(CreateVolumeBackupMsg msg, Map<String, Quota.QuotaPair> pairs) {
        String curAccount = msg.getAccountUuid();
        String ownerAccount = msg.getAccountUuid();
        String volumeUuid = msg.getVolumeUuid();
        String mode = msg.getMode().toString();

        checkVolumeBackupMsg(pairs, curAccount, ownerAccount, volumeUuid, mode);
    }

    @Transactional(readOnly = true)
    protected void check(CreateVmBackupMsg msg, Map<String, Quota.QuotaPair> pairs) {
        String curAccount = msg.getAccountUuid();
        String ownerAccount = msg.getAccountUuid();
        String volumeUuid = msg.getVolumeUuid();
        String mode = msg.getMode().toString();

        checkVmBackupMsg(pairs, curAccount, ownerAccount, volumeUuid, mode);
    }

    @Transactional(readOnly = true)
    protected void check(APICreateVolumeBackupMsg msg, Map<String, Quota.QuotaPair> pairs) {
        String curAccount = msg.getSession().getAccountUuid();
        String ownerAccount = msg.getSession().getAccountUuid();
        String volumeUuid = msg.getVolumeUuid();
        String mode = msg.getMode();

        checkVolumeBackupMsg(pairs, curAccount, ownerAccount, volumeUuid, mode);
    }

    @Transactional(readOnly = true)
    protected void check(APICreateVmBackupMsg msg, Map<String, Quota.QuotaPair> pairs) {
        String curAccount = msg.getSession().getAccountUuid();
        String ownerAccount = msg.getSession().getAccountUuid();
        String volumeUuid = msg.getVolumeUuid();
        String mode = msg.getMode();

        checkVmBackupMsg(pairs, curAccount, ownerAccount, volumeUuid, mode);
    }

    protected void checkVolumeBackupMsg(Map<String, Quota.QuotaPair> pairs, String curAccount, String ownerAccount, String volumeUuid, String mode) {
        VolumeBackupQuotaUtil.VolumeBackupQuota backupQuota = new VolumeBackupQuotaUtil().getUsed(ownerAccount);

        checkVolumeBackupNumQuota(1, backupQuota.backupNum, curAccount, ownerAccount, pairs);

        long requestCapacity;
        if (BackupMode.full.toString().equals(mode)) {
            requestCapacity = Q.New(VolumeVO.class)
                    .select(VolumeVO_.size)
                    .eq(VolumeVO_.uuid, volumeUuid)
                    .findValue();
        } else {
            //TODO: predict the size of a backup
            requestCapacity = 1;
        }
        checkVolumeBackupCapacityQuota(requestCapacity, backupQuota.backupSize, curAccount, ownerAccount, pairs);
    }

    protected void checkVmBackupMsg(Map<String, Quota.QuotaPair> pairs, String curAccount, String ownerAccount, String volumeUuid, String mode) {
        VmInstanceVO vmVO = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.rootVolumeUuid, volumeUuid)
                .find();

        VolumeBackupQuotaUtil.VolumeBackupQuota backupQuota = new VolumeBackupQuotaUtil().getUsed(ownerAccount);

        checkVolumeBackupNumQuota(vmVO.getAllVolumes().size(), backupQuota.backupNum, curAccount, ownerAccount, pairs);
        long requestCapacity;
        if (BackupMode.full.toString().equals(mode)) {
            requestCapacity = vmVO.getAllVolumes().stream().mapToLong(VolumeVO::getSize).sum();
        } else {
            //TODO: predict the size of a backup
            requestCapacity = 1;
        }
        checkVolumeBackupCapacityQuota(requestCapacity, backupQuota.backupSize, curAccount, ownerAccount, pairs);
    }

    @Transactional(readOnly = true)
    protected void check(APIChangeResourceOwnerMsg msg, Map<String, Quota.QuotaPair> pairs) {
        String curAccount = msg.getSession().getAccountUuid();
        String ownerAccount = msg.getAccountUuid();
        if (new QuotaUtil().isAdminAccount(ownerAccount)) {
            return;
        }

        VolumeBackupQuotaUtil.VolumeBackupQuota backupQuota = new VolumeBackupQuotaUtil().getUsed(ownerAccount);

        long count = ResourceHelper.countOwnResources(VolumeBackupVO.class, msg.getResourceUuid());
        if (count > 0) {
            checkVolumeBackupNumQuota(1, backupQuota.backupNum, curAccount, ownerAccount, pairs);

            VolumeBackupVO backup = dbf.getEntityManager().find(VolumeBackupVO.class, msg.getResourceUuid());
            checkVolumeBackupCapacityQuota(backup.getSize(), backupQuota.backupSize, curAccount, ownerAccount, pairs);
        }
    }

    @Transactional(readOnly = true)
    protected void checkVolumeBackupNumQuota(long requestNum, long usedNum, String curAccount,
                                             String ownerAccount, Map<String, Quota.QuotaPair> pairs) {
        long numQuota = pairs.get(VolumeBackupQuotaConstant.VOLUME_BACKUP_NUM).getValue();

        QuotaUtil.QuotaCompareInfo compareInfo = new QuotaUtil.QuotaCompareInfo();
        compareInfo.currentAccountUuid = curAccount;
        compareInfo.resourceTargetOwnerAccountUuid = ownerAccount;
        compareInfo.quotaName = VolumeBackupQuotaConstant.VOLUME_BACKUP_NUM;
        compareInfo.quotaValue = numQuota;
        compareInfo.request = requestNum;
        compareInfo.currentUsed = usedNum;
        new QuotaUtil().CheckQuota(compareInfo);
    }

    @Transactional(readOnly = true)
    protected void checkVolumeBackupCapacityQuota(long requestCapacity, long usedSize, String curAccount,
                                                  String ownerAccount, Map<String, Quota.QuotaPair> pairs) {
        long capacityQuota = pairs.get(VolumeBackupQuotaConstant.VOLUME_BACKUP_SIZE).getValue();

        QuotaUtil.QuotaCompareInfo compareInfo = new QuotaUtil.QuotaCompareInfo();
        compareInfo.currentAccountUuid = curAccount;
        compareInfo.resourceTargetOwnerAccountUuid = ownerAccount;
        compareInfo.quotaName = VolumeBackupQuotaConstant.VOLUME_BACKUP_SIZE;
        compareInfo.quotaValue = capacityQuota;
        compareInfo.request = requestCapacity;
        compareInfo.currentUsed = usedSize;
        new QuotaUtil().CheckQuota(compareInfo);
    }

    @Override
    public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
        List<Quota.QuotaUsage> usages = new ArrayList<>();

        VolumeBackupQuotaUtil.VolumeBackupQuota backupQuota = new VolumeBackupQuotaUtil().getUsed(accountUuid);
        Quota.QuotaUsage usage = new Quota.QuotaUsage();

        usage.setName(VolumeBackupQuotaConstant.VOLUME_BACKUP_NUM);
        usage.setUsed(backupQuota.backupNum);
        usages.add(usage);

        usage = new Quota.QuotaUsage();
        usage.setName(VolumeBackupQuotaConstant.VOLUME_BACKUP_SIZE);
        usage.setUsed(backupQuota.backupSize);
        usages.add(usage);

        return usages;
    }
}
