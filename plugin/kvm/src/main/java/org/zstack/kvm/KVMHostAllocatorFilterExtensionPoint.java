package org.zstack.kvm;

import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18m;

public class KVMHostAllocatorFilterExtensionPoint implements HostAllocatorFilterExtensionPoint {
    private CLogger logger = Utils.getLogger(KVMHostAllocatorFilterExtensionPoint.class);

    private static Map<KVMPropertyName, KVMPropertyChecker> propertyCheckerMap = new HashMap<>();

    enum KVMPropertyName {
        EPT("ept"),
        LIBVIRT_VERSION("libvirt version"),
        QEMU_IMG_VERSION("qemu version"),
        CPU_MODEL_NAME("cpu mode name");

        String name;

        KVMPropertyName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * interface for kvm property check
     */
    interface KVMPropertyChecker {
        /**
         * String value of KVMPropertyName matched property
         * currently only system tag offers properties
         * @param hostUuid host uuid need get property of
         * @return currently only system used to record
         * change the return type if needed
         */
        String getKVMHostProperty(String hostUuid);

        /**
         * return a defined value from KVMPropertyName
         * @return value from KVMPropertyName
         */
        KVMPropertyName getPropertyName();

        /**
         * used for condition check because some property
         * check could be control by global config in order
         * to not break the default usage
         * @return need check this property return true else false
         */
        default boolean needCheck() {
            return true;
        }
    }

    static class QemuImgVersionChecker implements KVMPropertyChecker {
        @Override
        public String getKVMHostProperty(String hostUuid) {
            return KVMSystemTags.QEMU_IMG_VERSION.getTokenByResourceUuid(hostUuid, KVMSystemTags.QEMU_IMG_VERSION_TOKEN);
        }

        @Override
        public KVMPropertyName getPropertyName() {
            return KVMPropertyName.QEMU_IMG_VERSION;
        }
    }

    static class LibvirtVersionChecker implements KVMPropertyChecker {
        @Override
        public String getKVMHostProperty(String hostUuid) {
            return KVMSystemTags.LIBVIRT_VERSION.getTokenByResourceUuid(hostUuid, KVMSystemTags.LIBVIRT_VERSION_TOKEN);
        }

        @Override
        public KVMPropertyName getPropertyName() {
            return KVMPropertyName.LIBVIRT_VERSION;
        }
    }

    static class CPUModelChecker implements KVMPropertyChecker {
        @Override
        public String getKVMHostProperty(String hostUuid) {
            return KVMSystemTags.CPU_MODEL_NAME.getTokenByResourceUuid(hostUuid, KVMSystemTags.CPU_MODEL_NAME_TOKEN);
        }

        @Override
        public KVMPropertyName getPropertyName() {
            return KVMPropertyName.CPU_MODEL_NAME;
        }

        @Override
        public boolean needCheck() {
            return KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME.value(Boolean.class);
        }
    }

    static class HostEPTChecker implements KVMPropertyChecker {
        @Override
        public String getKVMHostProperty(String hostUuid) {
            return KVMSystemTags.EPT_CPU_FLAG.getTokenByResourceUuid(hostUuid, KVMSystemTags.EPT_CPU_FLAG_TOKEN);
        }

        @Override
        public KVMPropertyName getPropertyName() {
            return KVMPropertyName.EPT;
        }
    }

    static {
        List<KVMPropertyChecker> checkers = new ArrayList<>();
        checkers.add(new QemuImgVersionChecker());
        checkers.add(new LibvirtVersionChecker());
        checkers.add(new CPUModelChecker());
        checkers.add(new HostEPTChecker());
        checkers.forEach(checker -> propertyCheckerMap.put(checker.getPropertyName(), checker));
    }

    private Map<KVMPropertyName, String> getPropertyMapOfHost(String hostUuid) {
        Map<KVMPropertyName, String> hostPropertyMap = new HashMap<>();
        propertyCheckerMap.forEach((key, checker) -> {
            if (!checker.needCheck()) {
                return;
            }

            hostPropertyMap.put(key, checker.getKVMHostProperty(hostUuid));
        });
        return hostPropertyMap;
    }

    private boolean allPropertiesMatched(Map<KVMPropertyName, String> srcHostProperties, Map<KVMPropertyName, String> dstHostProperties) {
        if (srcHostProperties == null || srcHostProperties.isEmpty()) {
            return true;
        }

        // find not empty property and compare with dest host
        List<Map.Entry<KVMPropertyName, String>> mismatchPropertyList = srcHostProperties.entrySet()
                .stream()
                .filter(entry -> !Objects.equals(entry.getValue(), dstHostProperties.get(entry.getKey())))
                .collect(Collectors.toList());

        if (mismatchPropertyList.isEmpty()) {
            return true;
        }

        // add debug for mismatch properties
        mismatchPropertyList.forEach(entry -> logger.debug(String.format("kvm host %s not match, src host[%s: %s], dst host [%s: %s]",
                entry.getKey().getName(),
                entry.getKey().getName(), entry.getValue(),
                entry.getKey().getName(), dstHostProperties.get(entry.getKey()))));

        return false;
    }

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        if (spec.getHypervisorType() == null || !spec.getHypervisorType().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            return;
        }

        if (!VmInstanceConstant.VmOperation.Migrate.toString().equals(spec.getVmOperation())) {
            return;
        }

        // note: for control plane use KVMPropertyName to define kvm host related property related to
        // live migration
        // vm can only live migrate to host that have the same version of qemu/libvirt, the
        // same cpu model name and same ept state
        String srcHostUuid = spec.getVmInstance().getHostUuid();
        // nothing to check
        Map<KVMPropertyName, String> srcHostPropertyMap = getPropertyMapOfHost(srcHostUuid);
        for (HostCandidate candidate : candidates) {
            Map<KVMPropertyName, String> candidateHostPropertyMap = getPropertyMapOfHost(candidate.getUuid());
            if (!allPropertiesMatched(srcHostPropertyMap, candidateHostPropertyMap)) {
                candidate.markAsRejected(getClass(),
                        i18m("kvm properties mismatch detected from host[uuid:%s]", srcHostUuid));
            }
        }
    }
}
