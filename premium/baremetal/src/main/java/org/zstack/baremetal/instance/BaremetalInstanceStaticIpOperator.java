package org.zstack.baremetal.instance;

import org.zstack.core.db.Q;
import org.zstack.header.baremetal.instance.BaremetalInstanceVO;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.TagUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by GuoYi on 7/11/18.
 */
public class BaremetalInstanceStaticIpOperator {
    Map<String, String> getStaticIpbyBmUuid(String bmUuid) {
        Map<String, String> ret = new HashMap<>();

        List<Map<String, String>> tokenList = BaremetalInstanceSystemTags.STATIC_IP.getTokensOfTagsByResourceUuid(bmUuid);
        for (Map<String, String> tokens : tokenList) {
            String l3Uuid = tokens.get(BaremetalInstanceSystemTags.STATIC_IP_L3_UUID_TOKEN);
            String ip = tokens.get(BaremetalInstanceSystemTags.STATIC_IP_TOKEN);
            ret.put(l3Uuid, ip);
        }

        return ret;
    }

    public void setStaticIp(String bmUuid, String l3Uuid, String ip) {
        final String tagUuid = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.uuid)
                .eq(SystemTagVO_.resourceUuid, bmUuid)
                .eq(SystemTagVO_.resourceType, BaremetalInstanceVO.class.getSimpleName())
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(
                        BaremetalInstanceSystemTags.STATIC_IP.instantiateTag(
                                map(e(BaremetalInstanceSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid)))))
                .findValue();
        if (tagUuid == null) {
            SystemTagCreator creator = BaremetalInstanceSystemTags.STATIC_IP.newSystemTagCreator(bmUuid);
            creator.setTagByTokens(map(
                    e(BaremetalInstanceSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                    e(BaremetalInstanceSystemTags.STATIC_IP_TOKEN, ip))
            );
            creator.create();
        } else {
            BaremetalInstanceSystemTags.STATIC_IP.updateByTagUuid(tagUuid, BaremetalInstanceSystemTags.STATIC_IP.instantiateTag(map(
                    e(BaremetalInstanceSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                    e(BaremetalInstanceSystemTags.STATIC_IP_TOKEN, ip)))
            );
        }
    }

    public void deleteStaticIpByBmUuidAndL3Uuid(String bmUuid, String l3Uuid) {
        BaremetalInstanceSystemTags.STATIC_IP.delete(
                bmUuid, TagUtils.tagPatternToSqlPattern(BaremetalInstanceSystemTags.STATIC_IP.instantiateTag(
                        map(e(BaremetalInstanceSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid))))
        );
    }

    public void deleteStaticIpByL3NetworkUuid(String l3Uuid) {
        BaremetalInstanceSystemTags.STATIC_IP.delete(null, BaremetalInstanceSystemTags.STATIC_IP.instantiateTag(map(
                e(BaremetalInstanceSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                e(BaremetalInstanceSystemTags.STATIC_IP_TOKEN, "%")))
        );
    }
}
