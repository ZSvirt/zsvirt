package org.zstack.network.service.vip;

import java.util.List;

public interface VipGetServiceReferencePoint {
    public final class ServiceReference{
        String useFor;
        long    count;
        List<String> serviceUids;

        public ServiceReference(String useFor, long peerL3Count, List<String> serviceUids) {
            this.useFor = useFor;
            this.count = peerL3Count;
            this.serviceUids = serviceUids;
        }

        public List<String> getUuids() {
            return serviceUids;
        }

        public void setUuids(List<String> serviceUids) {
            this.serviceUids = serviceUids;
        }

        public String getUseFor() {
            return useFor;
        }

        public void setUseFor(String useFor) {
            this.useFor = useFor;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    /*how many networks attached*/
    /* this api will return the active peerL3 count bound to this vip */
    ServiceReference getServiceReference(String vipUuid);

    /*this api will return the nic count with peer L3 bound to this vip*/
    ServiceReference getServicePeerL3Reference(String vipUuid, String peerL3Uuid);
}
