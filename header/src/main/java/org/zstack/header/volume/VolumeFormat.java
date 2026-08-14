package org.zstack.header.volume;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HypervisorType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class VolumeFormat {
    private static final CLogger logger = Utils.getLogger(VolumeFormat.class);
    private static Map<String, VolumeFormat> types = Collections.synchronizedMap(new HashMap<String, VolumeFormat>());
    private final String typeName;
    private final HypervisorType masterHypervisorType;
    private final List<HypervisorType> attachableHypervisorTypes = new ArrayList<HypervisorType>();
    private final Map<HypervisorType, Map<String, String>> inputOuputFormatsMap = new HashMap<HypervisorType, Map<String, String>>();
    private boolean exposed = true;
    private HypervisorType firstChoice;

    public static Collection<VolumeFormat> getAllFormats() {
        HashSet<VolumeFormat> exposedFormats = new HashSet<VolumeFormat>();
        for (VolumeFormat format : types.values()) {
            if (format.isExposed()) {
                exposedFormats.add(format);
            }
        }
        return exposedFormats;
    }

    public HypervisorType getFirstChoice() {
        return firstChoice;
    }

    public void setFirstChoice(HypervisorType firstChoice) {
        this.firstChoice = firstChoice;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public VolumeFormat(String typeName, HypervisorType master, HypervisorType... attachables) {
        this.typeName = typeName;
        types.put(typeName, this);
        masterHypervisorType = master;
        Collections.addAll(attachableHypervisorTypes, attachables);
        logger.debug(String.format("volume format[%s] registers itself with master hypervisor type[%s] and attachable hypervisor types%s", typeName, masterHypervisorType, attachableHypervisorTypes));
    }

    public VolumeFormat(String typeName, HypervisorType master, boolean exposed, HypervisorType... attachables) {
        this(typeName, master, attachables);
        this.exposed = exposed;
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static VolumeFormat valueOf(String typeName) {
        VolumeFormat type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException(String.format("cannot find VolumeFormat[%s]", typeName));
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof VolumeFormat)) {
            return false;
        }

        VolumeFormat type = (VolumeFormat) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        return types.keySet();
    }

    public HypervisorType getMasterHypervisorType() {
        return masterHypervisorType;
    }

    public List<HypervisorType> getAttachableHypervisorTypes() {
        return attachableHypervisorTypes;
    }

    public static HypervisorType getMasterHypervisorTypeByVolumeFormat(String volumeFormat) {
        VolumeFormat f = VolumeFormat.valueOf(volumeFormat);
        return f.getMasterHypervisorType();
    }

    public static VolumeFormat getVolumeFormatByMasterHypervisorType(String hvType) {
        for (VolumeFormat f : types.values()) {
            if (f.getFirstChoice() != null && f.getFirstChoice().toString().equals(hvType)) {
                return f;
            }
        }

        for (VolumeFormat f : types.values()) {
            if (f.getMasterHypervisorType() != null && f.getMasterHypervisorType().toString().equals(hvType)) {
                return f;
            }
        }

        for (VolumeFormat f : types.values()) {
            if (f.getAttachableHypervisorTypes() != null && f.getAttachableHypervisorTypes().contains(HypervisorType.valueOf(hvType))) {
                return f;
            }
        }

        throw new CloudRuntimeException(String.format("cannot find volume format which has master hypervisor type[%s]", hvType));
    }

    public List<String> getHypervisorTypesSupportingThisVolumeFormatInString() {
        return transformAndRemoveNull(getHypervisorTypesSupportingThisVolumeFormat(), HypervisorType::toString);
    }

    public List<HypervisorType> getHypervisorTypesSupportingThisVolumeFormat() {
        List<HypervisorType> hvTypes = new ArrayList<HypervisorType>();
        if (masterHypervisorType != null) {
            hvTypes.add(masterHypervisorType);
        }
        hvTypes.addAll(attachableHypervisorTypes);
        return hvTypes;
    }

    public static List<VolumeFormat> getVolumeFormatSupportedByHypervisorType(HypervisorType hvType) {
        List<VolumeFormat> formats = new ArrayList<VolumeFormat>();
        for (VolumeFormat format : types.values()) {
            if (hvType.equals(format.getMasterHypervisorType()) || format.getAttachableHypervisorTypes().contains(hvType)) {
                formats.add(format);
            }
        }
        return formats;
    }

    public static List<VolumeFormat> getVolumeFormatSupportedByHypervisorType(String hvType) {
        return getVolumeFormatSupportedByHypervisorType(HypervisorType.valueOf(hvType));
    }

    public static List<String> getVolumeFormatSupportedByHypervisorTypeInString(HypervisorType hvType) {
        return transformAndRemoveNull(getVolumeFormatSupportedByHypervisorType(hvType), VolumeFormat::toString);
    }

    public static List<String> getVolumeFormatSupportedByHypervisorTypeInString(String hvType) {
        return getVolumeFormatSupportedByHypervisorTypeInString(HypervisorType.valueOf(hvType));
    }

    public void newFormatInputOutputMapping(HypervisorType hvType, String output) {
        Map<String, String> io = inputOuputFormatsMap.get(hvType);
        if (io == null) {
            io = new HashMap<String, String>();
            inputOuputFormatsMap.put(hvType, io);
        }
        io.put(toString(), output);
    }

    public String getOutputFormat(String hvType) {
        return getOutputFormat(HypervisorType.valueOf(hvType));
    }

    public String getOutputFormat(HypervisorType hvType) {
        Map<String, String> io = inputOuputFormatsMap.get(hvType);
        if (io == null) {
            return toString();
        }

        String output = io.get(toString());
        return output == null ? toString() : output;
    }

    public void addAttachableHypervisorTypesToVolumeFormat(HypervisorType hypervisorType) {
        attachableHypervisorTypes.add(hypervisorType);
    }
}
