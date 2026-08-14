package org.zstack.mevoco;

import org.apache.commons.lang.math.NumberUtils;
import org.zstack.core.Platform;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.volume.VolumeQos;
import org.zstack.header.volume.VolumeQosMode;
import org.zstack.header.volume.VolumeQosType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.volume.MevocoVolumeSystemTags;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;

import java.util.HashMap;
import java.util.Map;

import static org.zstack.header.volume.VolumeQosConstant.*;
import static org.zstack.header.volume.VolumeQosMode.*;
import static org.zstack.header.volume.VolumeQosType.BANDWIDTH;
import static org.zstack.header.volume.VolumeQosType.IOPS;
import static org.zstack.mevoco.MevocoConstants.KVM_VOLUME_QOS;

/**
 * Created by mingjian.deng on 2018/11/19.
 */

public class VolumeQosHelper {
    public static String getVolumeQosString(VolumeQos qos) {
        if (qos == null) {
            return null;
        }
        StringBuilder buffer = new StringBuilder();
        if (qos.getReadBandwidth() != -1) {
            buffer.append(BANDWIDTH_READ);
            buffer.append('=');
            buffer.append(qos.getReadBandwidth());
            buffer.append(',');
        }

        if (qos.getWriteBandwidth() != -1) {
            buffer.append(BANDWIDTH_WRITE);
            buffer.append('=');
            buffer.append(qos.getWriteBandwidth());
            buffer.append(',');
        }

        if (qos.getTotalBandwidth() != -1) {
            buffer.append(BANDWIDTH_TOTAL);
            buffer.append('=');
            buffer.append(qos.getTotalBandwidth());
            buffer.append(',');
        }

        if (qos.getReadIOPS() != -1) {
            buffer.append(IOPS_READ);
            buffer.append('=');
            buffer.append(qos.getReadIOPS());
            buffer.append(',');
        }

        if (qos.getWriteIOPS() != -1) {
            buffer.append(IOPS_WRITE);
            buffer.append('=');
            buffer.append(qos.getWriteIOPS());
            buffer.append(',');
        }

        if (qos.getTotalIOPS() != -1) {
            buffer.append(IOPS_TOTAL);
            buffer.append('=');
            buffer.append(qos.getTotalIOPS());
            buffer.append(',');
        }

        if (buffer.length() > 0) {
            buffer.deleteCharAt(buffer.lastIndexOf(","));
            return buffer.toString();
        } else {
            // if all == -1, return null
            return null;
        }
    }

    /**
     * This method only use by set qos V1
     * @param oldQos
     * @param newQos merge qos
     * @return can be set to VolumeVO directly
     */
    public static String mergeVolumeQosAndGetString(VolumeQos oldQos, VolumeQos newQos) {
        if (newQos == null) {
            return getVolumeQosString(oldQos);
        }

        if (oldQos == null) {
            return getVolumeQosString(newQos);
        } else if (oldQos.getTotalBandwidth() != -1 || newQos.getTotalBandwidth() != -1) {
            // if old / new qos in total mode, then we don't need merge qos
            // V1 msg cannot set iops, simply copy old values.
            newQos.setTotalIOPS(oldQos.getTotalIOPS());
            newQos.setWriteIOPS(oldQos.getWriteIOPS());
            newQos.setReadIOPS(oldQos.getReadIOPS());
            return getVolumeQosString(newQos);
        }

        VolumeQos qos = new VolumeQos(oldQos.getReadBandwidth(), oldQos.getWriteBandwidth(), -1L,
                oldQos.getReadIOPS(), oldQos.getWriteIOPS(), oldQos.getTotalIOPS());

        if (newQos.getReadBandwidth() != -1) {
            qos.setReadBandwidth(newQos.getReadBandwidth());
        }

        if (newQos.getWriteBandwidth() != -1) {
            qos.setWriteBandwidth(newQos.getWriteBandwidth());
        }

        return getVolumeQosString(qos);
    }

    public static VolumeQos getVolumeQos(Long totalQosInput, Long writeQosInput, Long readQosInput, Long totalIOPSInput, Long writeIOPSInput, Long readIOPSInput) {
        VolumeQos qos = new VolumeQos();
        if (totalQosInput != null) {
            qos.setTotalBandwidth(totalQosInput);
        }

        if (writeQosInput != null) {
            qos.setWriteBandwidth(writeQosInput);
        }

        if (readQosInput != null) {
            qos.setReadBandwidth(readQosInput);
        }

        if (totalIOPSInput != null) {
            qos.setTotalIOPS(totalIOPSInput);
        }

        if (writeIOPSInput != null) {
            qos.setWriteIOPS(writeIOPSInput);
        }

        if (readIOPSInput != null) {
            qos.setReadIOPS(readIOPSInput);
        }

        return qos;
    }

    public static VolumeQos getVolumeQos(String voQos) {
        return VolumeQos.valueOf(voQos);
    }

    public static VolumeQos getVolumeQos(Long vqos, VolumeQosMode mode) {
        VolumeQos qos = new VolumeQos();
        if (null == mode || TOTAL == mode) {
            qos.setTotalBandwidth(vqos);
        } else if (READ == mode) {
            qos.setReadBandwidth(vqos);
        } else if (WRITE == mode) {
            qos.setWriteBandwidth(vqos);
        } else {
            throw new OperationFailureException(Platform.operr("invalid volume qos mode: %s", mode));
        }
        return qos;
    }

    public static VolumeQos getVolumeQosFromSystemTag(String bandwidthTag, String iopsTag) {
        VolumeQos result = new VolumeQos();
        setVolumeQosFromTag(BANDWIDTH, bandwidthTag, result);
        setVolumeQosFromTag(IOPS, iopsTag, result);
        return result;
    }

    private static void setVolumeQosFromTag(VolumeQosType type, String tag, VolumeQos qos) {
        if (type == null || tag == null || qos == null) {
            return;
        }
        Map<String, String> qosMap;
        if (IOPS == type) {
            qosMap = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_IOPS_UP_THRESHOLD.getTokensByTag(tag);
        } else if (BANDWIDTH == type) {
            qosMap = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD.getTokensByTag(tag);
        } else {
            return;
        }

        if (qosMap == null) {
            return;
        }

        String val;
        if (IOPS == type) {
            val = qosMap.get(MevocoVolumeSystemTags.VOLUME_TOTAL_IOPS_TOKEN);
            if (NumberUtils.isNumber(val)) {
                qos.setTotalIOPS(Long.parseLong(val));
            }
            val = qosMap.get(MevocoVolumeSystemTags.VOLUME_READ_IOPS_TOKEN);
            if (NumberUtils.isNumber(val)) {
                qos.setReadIOPS(Long.parseLong(val));
            }
            val = qosMap.get(MevocoVolumeSystemTags.VOLUME_WRITE_IOPS_TOKEN);
            if (NumberUtils.isNumber(val)) {
                qos.setWriteIOPS(Long.parseLong(val));
            }
        }

        val = qosMap.get(MevocoVolumeSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN);
        if (NumberUtils.isNumber(val)) {
            qos.setTotalBandwidth(Long.parseLong(val));
        }
        val = qosMap.get(MevocoVolumeSystemTags.VOLUME_READ_BANDWIDTH_TOKEN);
        if (NumberUtils.isNumber(val)) {
            qos.setReadBandwidth(Long.parseLong(val));
        }
        val = qosMap.get(MevocoVolumeSystemTags.VOLUME_WRITE_BANDWIDTH_TOKEN);
        if (NumberUtils.isNumber(val)) {
            qos.setWriteBandwidth(Long.parseLong(val));
        }

    }

    public static Long getVolumeQosByMode(String vQos, VolumeQosMode mode) {
        return getVolumeQosByMode(getVolumeQos(vQos), mode);
    }

    public static Long getVolumeQosByMode(VolumeQos oldQos, VolumeQosMode mode) {
        if (oldQos == null) {
            throw new OperationFailureException(Platform.operr("cannot find mode from null VolumeQos"));
        }
        if ((null == mode) || (TOTAL == mode)) {
            return oldQos.getTotalBandwidth();
        } else if (READ == mode) {
            return oldQos.getReadBandwidth();
        } else if (WRITE == mode) {
            return oldQos.getWriteBandwidth();
        } else {
            throw new OperationFailureException(Platform.operr("invalid volume qos mode: %s", mode));
        }
    }

    public static VolumeQos deleteQosByMode(VolumeQos oldQos, VolumeQosMode mode) {
        if (oldQos == null || OVERWRITE == mode) {
            return null;
        }
        VolumeQos qos = new VolumeQos(oldQos.getReadBandwidth(), oldQos.getWriteBandwidth(), oldQos.getTotalBandwidth(),
                oldQos.getReadIOPS(), oldQos.getWriteIOPS(), oldQos.getTotalIOPS()
        );
        if (mode == null || TOTAL == mode) {
            qos.setTotalBandwidth(-1L);
        } else if (READ == mode) {
            qos.setReadBandwidth(-1L);
        } else if (WRITE == mode) {
            qos.setWriteBandwidth(-1L);
        } else if (ALL == mode) {
            qos.setTotalBandwidth(-1L);
            qos.setReadBandwidth(-1L);
            qos.setWriteBandwidth(-1L);
        } else {
            throw new OperationFailureException(Platform.operr("invalid volume qos mode: %s", mode));
        }

        return qos;
    }

    public static String copyUpThresholdQosTagFromVolume(String srcVolumeUuid, String dstVolumeUuid) {
        if (MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD.hasTag(srcVolumeUuid)) {
            SystemTagCreator creator = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD.newSystemTagCreator(dstVolumeUuid);
            creator.recreate = true;
            creator.inherent = false;
            creator.tag = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD.getTag(srcVolumeUuid);

            creator.create();
            return creator.tag;
        } else {
            return null;
        }
    }

    public static String copyUpThresholdIopsTagFromVolume(String srcVolumeUuid, String dstVolumeUuid) {
        if (!MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_IOPS_UP_THRESHOLD.hasTag(srcVolumeUuid)) {
            return null;
        }
        SystemTagCreator creator = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_IOPS_UP_THRESHOLD.newSystemTagCreator(
                dstVolumeUuid);
        creator.recreate = true;
        creator.inherent = false;
        creator.tag = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_IOPS_UP_THRESHOLD.getTag(srcVolumeUuid);
        creator.create();
        return creator.tag;
    }

    /**
     * Check if any qos limit is set
     * @param qos to be checked
     * @return true if any limit needs to be set, false if no limit is valid.
     */
    public static boolean hasQosLimit(VolumeQos qos) {
        if (qos == null) {
            return false;
        }
        return !(qos.getTotalIOPS() == null || qos.getTotalIOPS() == -1)
                || !(qos.getReadIOPS() == null || qos.getReadIOPS() == -1)
                || !(qos.getWriteIOPS() == null || qos.getWriteIOPS() == -1)
                || !(qos.getTotalBandwidth() == null || qos.getTotalBandwidth() == -1)
                || !(qos.getReadBandwidth() == null || qos.getReadBandwidth() == -1)
                || !(qos.getWriteBandwidth() == null || qos.getWriteBandwidth() == -1);
    }

    public static String getVolumeQosStr(String resourceUuid, Class resourceClass) {
        StringBuilder str = new StringBuilder();
        doGetVolumeQosStr(resourceUuid, resourceClass, str, BANDWIDTH);
        doGetVolumeQosStr(resourceUuid, resourceClass, str, IOPS);
        return str.toString().trim();
    }

    private static void doGetVolumeQosStr(String resourceUuid, Class resourceClass, StringBuilder sb, VolumeQosType type) {
        Map<String, PatternedSystemTag> tagMap;
        String totalToken, readToken, writeToken;
        String totalKey, readKey, writeKey;
        if (type == IOPS) {
            tagMap = MevocoVolumeSystemTags.iopsSystemTagMap;
            totalToken = MevocoVolumeSystemTags.VOLUME_TOTAL_IOPS_TOKEN;
            readToken = MevocoVolumeSystemTags.VOLUME_READ_IOPS_TOKEN;
            writeToken = MevocoVolumeSystemTags.VOLUME_WRITE_IOPS_TOKEN;
            totalKey = IOPS_TOTAL;
            readKey = IOPS_READ;
            writeKey = IOPS_WRITE;
        } else if (type == BANDWIDTH) {
            tagMap = MevocoVolumeSystemTags.volumeSystemTagMap;
            totalToken = MevocoVolumeSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN;
            readToken = MevocoVolumeSystemTags.VOLUME_READ_BANDWIDTH_TOKEN;
            writeToken = MevocoVolumeSystemTags.VOLUME_WRITE_BANDWIDTH_TOKEN;
            totalKey = BANDWIDTH_TOTAL;
            readKey = BANDWIDTH_READ;
            writeKey = BANDWIDTH_WRITE;
        } else {
            return;
        }

        for (Map.Entry<String, PatternedSystemTag> entry : tagMap.entrySet()) {
            PatternedSystemTag qosTag = entry.getValue();
            if (qosTag.hasTag(resourceUuid, resourceClass)) {
                if (qosTag.getTagFormat().contains(totalToken)) {
                    String qos = qosTag.getTokenByResourceUuid(resourceUuid, resourceClass, totalToken);
                    if (qos != null && !qos.equals("-1") && NumberUtils.isNumber(qos)) {
                        appendVolumeQosStr(sb, String.format("%s=%s", totalKey, qos), ",");
                    }
                } else if (qosTag.getTagFormat().contains(readToken)) {
                    String qos = qosTag.getTokenByResourceUuid(resourceUuid, resourceClass, readToken);
                    if (qos != null && !qos.equals("-1") && NumberUtils.isNumber(qos)) {
                        appendVolumeQosStr(sb, String.format("%s=%s", readKey, qos), ",");
                    }
                } else if (qosTag.getTagFormat().contains(writeToken)) {
                    String qos = qosTag.getTokenByResourceUuid(resourceUuid, resourceClass, writeToken);
                    if (qos != null && !qos.equals("-1") && NumberUtils.isNumber(qos)) {
                        appendVolumeQosStr(sb, String.format("%s=%s", writeKey, qos), ",");
                    }
                }
            }
        }
    }

    private static String getMergedStr(String newStr, String originStr, VolumeQosMode mode) {
        Long oldValue = getVolumeQosByMode(originStr, mode);
        Long newValue = getVolumeQosByMode(newStr, mode);
        if (oldValue != -1) {
            return originStr.replace(String.valueOf(oldValue), String.valueOf(newValue));
        } else {
            return originStr + "," + newStr;
        }
    }

    public static String mergeVolumeQosStr(String resourceUuid, Class resourceClass, String originResourceUuid, Class originClass) {
        String newStr = getVolumeQosStr(resourceUuid, resourceClass);
        String originStr = getVolumeQosStr(originResourceUuid, originClass);
        if (newStr.isEmpty() && !originStr.isEmpty()) {
            return originStr;
        }

        if (!newStr.isEmpty() && originStr.isEmpty()) {
            return newStr;
        }

        if (newStr.isEmpty()) {
            // both empty
            return "";
        }

        if (resourceClass != VolumeVO.class) {
            return originStr;
        }

        if ((newStr.contains(TOTAL.getMode()) && !originStr.contains(TOTAL.getMode())) || (!newStr.contains(
                TOTAL.getMode()) && originStr.contains(TOTAL.getMode()))) {
            // if mode changed, just use newStr
            return newStr;
        } else if (newStr.contains(TOTAL.getMode()) && originStr.contains(TOTAL.getMode())) {
            // if both "total" mode, just use newStr
            return newStr;
        } else if (newStr.contains(WRITE.getMode()) && newStr.contains(READ.getMode())) {
            return newStr;
        } else if (newStr.contains(WRITE.getMode())) {
            // only write in newStr
            return getMergedStr(newStr, originStr, WRITE);
        } else if (newStr.contains(READ.getMode())) {
            // only read in newStr
            return getMergedStr(newStr, originStr, READ);
        } else {
            return originStr;
        }
    }

    private static void appendVolumeQosStr(StringBuilder str, String value, String spliter) {
        if (str.length() > 0) {
            str.append(spliter);
        }

        str.append(value);
    }

    public static void copyQosFromOffering(String offeringUuid, Class offeringClass, String volumeUuid) {
        Map<String, String> volumeQos = getVolumeQosSystemTagMap(offeringUuid, offeringClass,
                MevocoVolumeSystemTags.volumeSystemTagMap);
        if (!volumeQos.isEmpty()) {
            SystemTagCreator creator = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD.newSystemTagCreator(volumeUuid);
            creator.recreate = true;
            creator.inherent = false;
            creator.setTagByTokens(volumeQos);
            creator.create();
        } else {
            MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD.delete(volumeUuid, VolumeVO.class);
        }
    }

    public static void copyIopsQosFromOffering(String offeringUuid, Class offeringClass, String volumeUuid) {
        Map<String, String> iopsQos = getVolumeQosSystemTagMap(offeringUuid, offeringClass,
                MevocoVolumeSystemTags.iopsSystemTagMap);
        if (iopsQos.isEmpty()) {
            MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_IOPS_UP_THRESHOLD.delete(volumeUuid, VolumeVO.class);
            return;
        }
        SystemTagCreator creator = MevocoVolumeSystemTags.NORMAL_ACCOUNT_VOLUME_IOPS_UP_THRESHOLD.newSystemTagCreator(
                volumeUuid);
        creator.recreate = true;
        creator.inherent = false;
        creator.setTagByTokens(iopsQos);
        creator.create();
    }

    public static Map<String, String> getVolumeQosSystemTagMap(String offeringUuid, Class resourceClass,
            Map<String, PatternedSystemTag> tagMaps) {
        Map<String, String> tags = new HashMap<>();
        for (Map.Entry<String, PatternedSystemTag> entry : tagMaps.entrySet()) {
            PatternedSystemTag qosTag = entry.getValue();
            if (qosTag.hasTag(offeringUuid, resourceClass)) {
                tags.put(entry.getKey(), qosTag.getTokenByResourceUuid(offeringUuid, resourceClass, entry.getKey()));
            }
        }
        return tags;
    }

    public static void removeVolumeQosInOldVersion(String resourceUuid, Class resourceClass) {
        for (Map.Entry<String, PatternedSystemTag> entry : MevocoVolumeSystemTags.volumeSystemTagMap.entrySet()) {
            PatternedSystemTag qosTag = entry.getValue();
            if (qosTag.hasTag(resourceUuid, resourceClass)) {
                qosTag.delete(resourceUuid, resourceClass);
            }
        }
    }

    public static void setVolumeQosAddons(VolumeQos qos, String volumeUuid, Map<String, Object> addonMap) {
        Object obj = addonMap.get(KVM_VOLUME_QOS);
        HashMap<String, Object> qosMap;
        if (obj != null) {
            qosMap = (HashMap) obj;
        } else {
            qosMap = new HashMap<>();
        }
        qosMap.put(volumeUuid, qos);
        addonMap.put(KVM_VOLUME_QOS, qosMap);
    }
}
