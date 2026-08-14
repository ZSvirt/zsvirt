package org.zstack.kvm;

import org.apache.commons.codec.digest.DigestUtils;
import org.zstack.core.db.Q;
import org.zstack.header.network.l2.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.TagType;
import org.zstack.utils.TagUtils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by GuoYi on 4/16/20.
 */
public class KVMHostUtils {
    private static final CLogger logger = CLoggerImpl.getLogger(KVMHostUtils.class);

    /**
     * Get normalized bridge name for l2 network, which at most has 15 chars.
     * - if l2 network has L2_BRIDGE_NAME tag, then return it's value directly;
     * - if l2Uuid does not have an L2_BRIDGE_NAME tag and conflict with existing bridge name,
     * use the new naming convention : prefix 'l2_' plus the last 12 characters of l2Uuid;
     * - if l2 physical interface name is short, then no need for truncation;
     * - otherwise, get md5sum of interface name and use the top chars.
     * @param l2Uuid l2 network uuid
     * @param format bridge name format string, like "br_%s", "br_%s_100" (only one '%s' is allowed)
     * @return normalized bridge name, or null if anything wrong
     */
    public static String getNormalizedBridgeName(String l2Uuid, String format) {
        if (KVMSystemTags.L2_BRIDGE_NAME.hasTag(l2Uuid, L2NetworkVO.class)) {
            return KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Uuid, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        }

        validateFormatString(format);

        String newBridgeName = generateBridgeNameWithL2Uuid(l2Uuid);
        String physicalInterface = getPhysicalInterface(l2Uuid, format);

        return checkNameConflict(l2Uuid, String.format(format, physicalInterface)) ?
                newBridgeName : String.format(format, physicalInterface);
    }

    private static void validateFormatString(String format) {
        if (!format.contains("%s") || format.indexOf("%s") != format.lastIndexOf("%s")) {
            throw new IllegalArgumentException(String.format("invalid format string: %s", format));
        }
    }

    public static String generateBridgeNameWithL2Uuid(String l2Uuid) {
        return "br_" + l2Uuid.substring(0, Math.min(l2Uuid.length(), 12));
    }

    private static String getPhysicalInterface(String l2Uuid, String format) {
        String physicalInterface = Q.New(L2NetworkVO.class)
                .eq(L2NetworkVO_.uuid, l2Uuid)
                .select(L2NetworkVO_.physicalInterface)
                .findValue();

        int allowedLen = L2NetworkConstant.LINUX_IF_NAME_MAX_SIZE - format.length() + 2; // "%s" length is 2

        if (physicalInterface != null && physicalInterface.length() > allowedLen) {
            physicalInterface = DigestUtils.md5Hex(physicalInterface).substring(0, allowedLen);
        }
        return physicalInterface;
    }

    public static Boolean checkNameConflict(String l2Uuid, String bridgeName) {
        String pattern = TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.instantiateTag(
                Collections.singletonMap(KVMSystemTags.L2_BRIDGE_NAME_TOKEN, bridgeName)
        ));
        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class)
                .select(L2NetworkClusterRefVO_.clusterUuid)
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuid).listValues();
        if (clusterUuids.isEmpty()) {
            return Boolean.FALSE;
        }
        Set<String> relatedL2Uuids = new HashSet<>(Q.New(L2NetworkClusterRefVO.class)
                .select(L2NetworkClusterRefVO_.l2NetworkUuid)
                .in(L2NetworkClusterRefVO_.clusterUuid, clusterUuids).listValues());
        List<SystemTagVO> tags = Q.New(SystemTagVO.class)
                .in(SystemTagVO_.resourceUuid, relatedL2Uuids)
                .eq(SystemTagVO_.resourceType, L2NetworkVO.class.getSimpleName())
                .like(SystemTagVO_.tag, pattern)
                .eq(SystemTagVO_.type, TagType.System)
                .list();

        return !tags.isEmpty();
    }
}
