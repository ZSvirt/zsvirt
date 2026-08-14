package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 16/10/19.
 */
public class GetVolumeQosOnKVMHostReply extends MessageReply {
    private Long volumeBandwidth = -1L;
    private Long volumeBandwidthWrite = -1L;
    private Long volumeBandwidthRead = -1L;
    private Long iopsTotal = -1L;
    private Long iopsWrite = -1L;
    private Long iopsRead = -1L;

    public Long getVolumeBandwidth() {
        return volumeBandwidth;
    }

    public void setVolumeBandwidth(Long volumeBandwidth) {
        this.volumeBandwidth = volumeBandwidth;
    }

    public Long getVolumeBandwidthWrite() {
        return volumeBandwidthWrite;
    }

    public void setVolumeBandwidthWrite(Long volumeBandwidthWrite) {
        this.volumeBandwidthWrite = volumeBandwidthWrite;
    }

    public Long getVolumeBandwidthRead() {
        return volumeBandwidthRead;
    }

    public void setVolumeBandwidthRead(Long volumeBandwidthRead) {
        this.volumeBandwidthRead = volumeBandwidthRead;
    }

    public Long getIopsTotal() {
        return iopsTotal;
    }

    public void setIopsTotal(Long iopsTotal) {
        this.iopsTotal = iopsTotal;
    }

    public Long getIopsWrite() {
        return iopsWrite;
    }

    public void setIopsWrite(Long iopsWrite) {
        this.iopsWrite = iopsWrite;
    }

    public Long getIopsRead() {
        return iopsRead;
    }

    public void setIopsRead(Long iopsRead) {
        this.iopsRead = iopsRead;
    }
}
