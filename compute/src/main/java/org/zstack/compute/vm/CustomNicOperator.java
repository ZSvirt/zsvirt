package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.TagUtils;

import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class CustomNicOperator {
    private final String vmUuid;
    private final String l3Uuid;
    public CustomNicOperator(String vmUuid,String l3Uuid){
        this.vmUuid = vmUuid;
        this.l3Uuid = l3Uuid;
    }
    public String getCustomNicId() {
        List<Map<String, String>> tokenList = VmSystemTags.CUSTOM_NIC_UUID.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            if (StringUtils.equals(tokens.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN), l3Uuid)) {
                return tokens.get(VmSystemTags.NIC_UUID_TOKEN);
            }
        }
        return null;
    }

    public void deleteNicTags() {
        VmSystemTags.CUSTOM_MAC.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.CUSTOM_MAC.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid))
        )));
        VmSystemTags.STATIC_IP.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_IP.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid))
        )));
        VmSystemTags.CUSTOM_NIC_UUID.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.CUSTOM_NIC_UUID.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid))
        )));
    }

    public void updateNicTags(String mac ,String ip,String nicUuid){
        // set the results to system tag
        // because MacOperator doesn't provide a set mac method , so we set it here using SystemTagCreator
        SystemTagCreator macCreator = VmSystemTags.CUSTOM_MAC.newSystemTagCreator(vmUuid);
        macCreator.ignoreIfExisting = false;
        macCreator.inherent = false;
        macCreator.recreate = true;
        macCreator.setTagByTokens(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                e(VmSystemTags.MAC_TOKEN, mac)
        ));
        if (mac != null) {
            macCreator.create();
        }

        SystemTagCreator ipTagCreator = VmSystemTags.STATIC_IP.newSystemTagCreator(vmUuid);
        ipTagCreator.ignoreIfExisting = false;
        ipTagCreator.inherent = false;
        ipTagCreator.recreate = true;
        ipTagCreator.setTagByTokens(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                e(VmSystemTags.STATIC_IP_TOKEN, ip)
        ));
        if (ip != null) {
            ipTagCreator.create();
        }

        SystemTagCreator nicIdCreator = VmSystemTags.CUSTOM_NIC_UUID.newSystemTagCreator(vmUuid);
        nicIdCreator.ignoreIfExisting = false;
        nicIdCreator.inherent = false;
        nicIdCreator.recreate = true;
        nicIdCreator.setTagByTokens(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                e(VmSystemTags.NIC_UUID_TOKEN, nicUuid)
        ));
        if (nicUuid != null) {
            nicIdCreator.create();
        }
    }
}
