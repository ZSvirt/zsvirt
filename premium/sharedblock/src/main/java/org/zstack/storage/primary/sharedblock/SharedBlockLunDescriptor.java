package org.zstack.storage.primary.sharedblock;

public class SharedBlockLunDescriptor {
    private String wwid;
    private Long capacityBytes;

    public String getWwid() {
        return wwid;
    }

    public void setWwid(String wwid) {
        this.wwid = wwid;
    }

    public Long getCapacityBytes() {
        return capacityBytes;
    }

    public void setCapacityBytes(Long capacityBytes) {
        this.capacityBytes = capacityBytes;
    }

    public static SharedBlockLunDescriptor __example__() {
        SharedBlockLunDescriptor lun = new SharedBlockLunDescriptor();
        lun.setWwid("36001405007d580afa1f4ecb818ad1bd2");
        lun.setCapacityBytes(1099511627776L);
        return lun;
    }
}
