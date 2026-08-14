package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;

import java.util.*;

/**
 * Created by xing5 on 2016/4/22.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UserdataBuilder {
    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;

    private String sshkeyRootPassword(List<String> sshKeys, String rootPassword) {
        StringBuilder sb = new StringBuilder();
        if (!sshKeys.isEmpty()) {
            sb.append("\nssh_authorized_keys:");
            sshKeys.forEach(sshKey -> {
                if (sshKey != null) {
                    sb.append(String.format("\n  - %s", sshKey));
                }
            });
            sb.append("\ndisable_root: false");
        }

        if (rootPassword != null) {
            sb.append("\nchpasswd:");
            sb.append("\n  list: |");
            sb.append(String.format("\n    root:%s", rootPassword));
            sb.append("\n  expire: False");
        }

        return sb.toString();
    }

    public List<String> buildByVmUuid(String vmUuid) {
        List<String> userdataList = new ArrayList<>();

        String userdata = VmSystemTags.USERDATA.getTokenByResourceUuid(vmUuid, VmSystemTags.USERDATA_TOKEN);
        if (userdata != null) {
            userdata = new String(Base64.getDecoder().decode(userdata.getBytes()));
            userdataList.add(userdata);
        }

        List<String> sshKeys = extEmitter.fetchAssociatedSshKeyPairs(vmUuid);

        String sshKey = VmSystemTags.SSHKEY.getTokenByResourceUuid(vmUuid, VmSystemTags.SSHKEY_TOKEN);
        if (sshKey != null) {
            sshKeys.add(sshKey);
        }
        String rootPassword = VmSystemTags.ROOT_PASSWORD.getTokenByResourceUuid(vmUuid, VmSystemTags.ROOT_PASSWORD_TOKEN);
        if (!sshKeys.isEmpty() || rootPassword != null) {
            userdataList.add(String.format("#cloud-config%s", sshkeyRootPassword(sshKeys, rootPassword)));
        }

        return userdataList;
    }

    public Map<String, List<String>> buildByVmUuids(List<String> vmUuids) {
        Map<String, List<String>> ret = new HashMap<>();
        for (String vmUuid : vmUuids) {
            ret.put(vmUuid, buildByVmUuid(vmUuid));
        }
        return ret;
    }
}
