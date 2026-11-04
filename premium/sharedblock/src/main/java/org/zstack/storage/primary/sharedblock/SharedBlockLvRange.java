package org.zstack.storage.primary.sharedblock;

public class SharedBlockLvRange {
    private String wwid;
    private Long lvOffsetBytes;
    private Long lunOffsetBytes;
    private Long lengthBytes;

    public String getWwid() {
        return wwid;
    }

    public void setWwid(String wwid) {
        this.wwid = wwid;
    }

    public Long getLvOffsetBytes() {
        return lvOffsetBytes;
    }

    public void setLvOffsetBytes(Long lvOffsetBytes) {
        this.lvOffsetBytes = lvOffsetBytes;
    }

    public Long getLunOffsetBytes() {
        return lunOffsetBytes;
    }

    public void setLunOffsetBytes(Long lunOffsetBytes) {
        this.lunOffsetBytes = lunOffsetBytes;
    }

    public Long getLengthBytes() {
        return lengthBytes;
    }

    public void setLengthBytes(Long lengthBytes) {
        this.lengthBytes = lengthBytes;
    }

    public static SharedBlockLvRange __example__() {
        SharedBlockLvRange range = new SharedBlockLvRange();
        range.setWwid("36001405007d580afa1f4ecb818ad1bd2");
        range.setLvOffsetBytes(0L);
        range.setLunOffsetBytes(5045481472L);
        range.setLengthBytes(205520896L);
        return range;
    }
}
