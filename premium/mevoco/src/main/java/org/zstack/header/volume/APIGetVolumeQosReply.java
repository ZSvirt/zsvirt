package org.zstack.header.volume;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 16/12/9.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetVolumeQosReply extends APIReply {
    private String volumeUuid;
    private long volumeBandwidth = -1;
    private long volumeBandwidthRead = -1;
    private long volumeBandwidthWrite = -1;
    private long iopsTotal = -1;
    private long iopsRead = -1;
    private long iopsWrite = -1;

    private long volumeBandwidthUpthreshold = -1;
    private long volumeBandwidthReadUpthreshold = -1;
    private long volumeBandwidthWriteUpthreshold = -1;
    private long iopsTotalUpthreshold = -1;
    private long iopsReadUpthreshold = -1;
    private long iopsWriteUpthreshold = -1;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public long getVolumeBandwidth() {
        return volumeBandwidth;
    }


    public void setVolumeBandwidth(long volumeBandwidth) {
        this.volumeBandwidth = volumeBandwidth;
    }

    public long getVolumeBandwidthRead() {
        return volumeBandwidthRead;
    }

    public void setVolumeBandwidthRead(long volumeBandwidthRead) {
        this.volumeBandwidthRead = volumeBandwidthRead;
    }

    public long getVolumeBandwidthWrite() {
        return volumeBandwidthWrite;
    }

    public void setVolumeBandwidthWrite(long volumeBandwidthWrite) {
        this.volumeBandwidthWrite = volumeBandwidthWrite;
    }

    public long getVolumeBandwidthUpthreshold() {
        return volumeBandwidthUpthreshold;
    }

    public void setVolumeBandwidthUpthreshold(long volumeBandwidthUpthreshold) {
        this.volumeBandwidthUpthreshold = volumeBandwidthUpthreshold;
    }

    public long getVolumeBandwidthReadUpthreshold() {
        return volumeBandwidthReadUpthreshold;
    }

    public void setVolumeBandwidthReadUpthreshold(long volumeBandwidthReadUpthreshold) {
        this.volumeBandwidthReadUpthreshold = volumeBandwidthReadUpthreshold;
    }

    public long getVolumeBandwidthWriteUpthreshold() {
        return volumeBandwidthWriteUpthreshold;
    }

    public void setVolumeBandwidthWriteUpthreshold(long volumeBandwidthWriteUpthreshold) {
        this.volumeBandwidthWriteUpthreshold = volumeBandwidthWriteUpthreshold;
    }

    public long getIopsTotal() {
        return iopsTotal;
    }

    public void setIopsTotal(long iopsTotal) {
        this.iopsTotal = iopsTotal;
    }

    public long getIopsRead() {
        return iopsRead;
    }

    public void setIopsRead(long iopsRead) {
        this.iopsRead = iopsRead;
    }

    public long getIopsWrite() {
        return iopsWrite;
    }

    public void setIopsWrite(long iopsWrite) {
        this.iopsWrite = iopsWrite;
    }

    public long getIopsTotalUpthreshold() {
        return iopsTotalUpthreshold;
    }

    public void setIopsTotalUpthreshold(long iopsTotalUpthreshold) {
        this.iopsTotalUpthreshold = iopsTotalUpthreshold;
    }

    public long getIopsReadUpthreshold() {
        return iopsReadUpthreshold;
    }

    public void setIopsReadUpthreshold(long iopsReadUpthreshold) {
        this.iopsReadUpthreshold = iopsReadUpthreshold;
    }

    public long getIopsWriteUpthreshold() {
        return iopsWriteUpthreshold;
    }

    public void setIopsWriteUpthreshold(long iopsWriteUpthreshold) {
        this.iopsWriteUpthreshold = iopsWriteUpthreshold;
    }

    public static APIGetVolumeQosReply __example__() {
        APIGetVolumeQosReply reply = new APIGetVolumeQosReply();
        reply.setVolumeBandwidth(100000L);
        reply.setVolumeBandwidthRead(-1);
        reply.setVolumeBandwidthWrite(-1);
        reply.setIopsTotal(-1);
        reply.setIopsRead(10000L);
        reply.setIopsWrite(10000L);
        reply.setVolumeBandwidthUpthreshold(200000L);
        reply.setVolumeBandwidthReadUpthreshold(-1);
        reply.setVolumeBandwidthWriteUpthreshold(-1);
        reply.setIopsTotalUpthreshold(-1);
        reply.setIopsReadUpthreshold(20000L);
        reply.setIopsWriteUpthreshold(15000L);
        reply.setVolumeUuid(uuid());
        return reply;
    }

}
