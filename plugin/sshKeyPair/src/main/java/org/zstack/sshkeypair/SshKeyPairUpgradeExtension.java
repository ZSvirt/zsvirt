package org.zstack.sshkeypair;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.sshkeypair.*;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.identity.ResourceHelper;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.*;

public class SshKeyPairUpgradeExtension implements Component {
    @Autowired
    DatabaseFacade dbf;
    private void upgradeVmInstanceSshKey() {
        Map<String, List<String>> accountVmInstanceMap = new HashMap<String, List<String>>();

        Long count = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .count();

        SQL.New("select ref.accountUuid, ref.resourceUuid " +
                "from AccountResourceRefVO ref where ref.resourceType = :resourceType and ref.type = :type", Tuple.class)
                .param("resourceType", VmInstanceVO.class.getSimpleName())
                .param("type", AccessLevel.Own)
                .limit(1000)
                .paginate(count, (List<Tuple> vmResources) -> {
                    for (Tuple vmResource : vmResources) {
                        String accountUuid = (String) vmResource.get(0);
                        String vmInstanceUuid = (String) vmResource.get(1);
                        accountVmInstanceMap.computeIfAbsent(accountUuid, k -> new ArrayList<>()).add(vmInstanceUuid);
                    }
        });

        for (Map.Entry<String, List<String>> entry : accountVmInstanceMap.entrySet()) {
            String accountUuid = entry.getKey();
            List<String> vmInstanceUuids = entry.getValue();
            upgradeSshKeyByAccount(accountUuid, vmInstanceUuids);
        }
    }

    private void upgradeSshKeyByAccount(String accountUuid, List<String> vmInstanceUuids) {
        List<SshKeyPairVO> sshKeyPairVOS = new ArrayList<>();
        List<SshKeyPairRefVO> sshKeyPairRefVOS = new ArrayList<>();

        Map<String, List<String>> sshKeyVmInstanceMap = new HashMap<String, List<String>>();
        vmInstanceUuids.forEach(vmInstanceUuid -> {
            String sshKey = VmSystemTags.SSHKEY.getTokenByResourceUuid(vmInstanceUuid, VmSystemTags.SSHKEY_TOKEN);
            if (sshKey != null) {
                sshKeyVmInstanceMap.computeIfAbsent(sshKey, k -> new ArrayList<>()).add(vmInstanceUuid);
            }
        });

        Integer sshKeyIndex = 0;
        for (Map.Entry<String, List<String>> entry : sshKeyVmInstanceMap.entrySet()) {
            String sshKey = entry.getKey();
            List<String> vmUuids = entry.getValue();

            // NOTE(ywang): Check the ssh key pair was uploaded by the account
            String sshKeyPairUuid = null;
            List<String> sshKeyPairUuids = ResourceHelper.findOwnResourceUuidList(SshKeyPairVO.class, accountUuid);
            if (!sshKeyPairUuids.isEmpty()) {
                sshKeyPairUuid = Q.New(SshKeyPairVO.class)
                        .select(SshKeyPairVO_.uuid)
                        .in(SshKeyPairVO_.uuid, sshKeyPairUuids)
                        .eq(SshKeyPairVO_.publicKey, sshKey)
                        .findValue();
            }

            if (sshKeyPairUuid == null) {
                sshKeyIndex += 1;
                sshKeyPairUuid = Platform.getUuid();
                SshKeyPairVO sshKeyPairVO = new SshKeyPairVO();
                sshKeyPairVO.setName(String.format("ssh_key_%s", sshKeyIndex));
                sshKeyPairVO.setUuid(sshKeyPairUuid);
                sshKeyPairVO.setAccountUuid(accountUuid);
                sshKeyPairVO.setPublicKey(sshKey);
                sshKeyPairVO.setCreateDate(new Timestamp(new Date().getTime()));
                sshKeyPairVOS.add(sshKeyPairVO);
            }

            for (String vmUuid : vmUuids) {
                // NOTE(ywang): Check the key was already associated to the vm
                boolean isExist = Q.New(SshKeyPairRefVO.class)
                        .eq(SshKeyPairRefVO_.sshKeyPairUuid, sshKeyPairUuid)
                        .eq(SshKeyPairRefVO_.resourceUuid, vmUuid)
                        .eq(SshKeyPairRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                        .isExists();
                if (!isExist) {
                    SshKeyPairRefVO sshKeyPairRefVO = new SshKeyPairRefVO();
                    sshKeyPairRefVO.setSshKeyPairUuid(sshKeyPairUuid);
                    sshKeyPairRefVO.setResourceUuid(vmUuid);
                    sshKeyPairRefVO.setResourceType(VmInstanceVO.class.getSimpleName());
                    sshKeyPairRefVO.setCreateDate(new Timestamp(new Date().getTime()));
                    sshKeyPairRefVOS.add(sshKeyPairRefVO);
                }
            }
        }

        if (!sshKeyPairVOS.isEmpty()) {
            dbf.persistCollection(sshKeyPairVOS);
        }
        if (!sshKeyPairRefVOS.isEmpty()) {
            dbf.persistCollection(sshKeyPairRefVOS);
        }
    }

    @Override
    public boolean start() {
        if (SshKeyPairGlobalProperty.UPGRADE_SSH_KEY_PAIR_FROM_SYSTEM_TAG) {
            upgradeVmInstanceSshKey();
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
