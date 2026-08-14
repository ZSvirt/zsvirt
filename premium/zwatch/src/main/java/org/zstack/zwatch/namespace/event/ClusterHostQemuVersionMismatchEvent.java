package org.zstack.zwatch.namespace.event;

import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.hypervisor.events.ClusterHostHypervisorMismatchData;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.EventFamily.Event;

/**
 * An event of cluster host QEMU version mismatch
 * 
 * Created by Wenhao.Zhang on 23/03/06
 */
public class ClusterHostQemuVersionMismatchEvent {
    private ClusterHostQemuVersionMismatchEvent() {}

    public static void installEventCollector(EventFamily eventFamily) {
        eventFamily.onCanonicalEvent(
                ClusterHostHypervisorMismatchData.PATH,
                ClusterHostQemuVersionMismatchEvent::collect);
    }

    private static Event collect(Object canonicalEvent) {
        ClusterHostHypervisorMismatchData data = (ClusterHostHypervisorMismatchData) canonicalEvent;
        if (!KVMConstant.VIRTUALIZER_QEMU_KVM.equals(data.getHypervisorType())) {
            return null;
        }

        return new Event(
                data.getClusterUuid(),
                Integer.toString(data.getHostCount()));
    }
}
