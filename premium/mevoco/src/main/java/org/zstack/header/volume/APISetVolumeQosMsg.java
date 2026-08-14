package org.zstack.header.volume;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

import static org.zstack.header.volume.MevocoVolumeConstants.*;

/**
 * Created by mingjian.deng on 16/12/9.
 */
@RestRequest(
        path = "/volumes/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVolumeQosEvent.class
)
public class APISetVolumeQosMsg extends APIMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String uuid;

    @APIParam(validValues={VOLUME_QOS_MODE_TOTAL, VOLUME_QOS_MODE_READ, VOLUME_QOS_MODE_WRITE}, required=false)
    private String mode = null;

    @APIParam(numberRange={1024, Long.MAX_VALUE}, required = false)
    private Long volumeBandwidth;

    @APIParam(numberRange = {1024, Long.MAX_VALUE}, required = false)
    private Long readBandwidth;

    @APIParam(numberRange = {1024, Long.MAX_VALUE}, required = false)
    private Long writeBandwidth;

    @APIParam(numberRange = {1024, Long.MAX_VALUE}, required = false)
    private Long totalBandwidth;

    @APIParam(numberRange = {1, Long.MAX_VALUE}, required = false)
    private Long readIOPS;

    @APIParam(numberRange = {1, Long.MAX_VALUE}, required = false)
    private Long writeIOPS;

    @APIParam(numberRange = {1, Long.MAX_VALUE}, required = false)
    private Long totalIOPS;

    @APINoSee
    private Integer version;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = StringUtils.isNotEmpty(mode) ? mode : VOLUME_QOS_MODE_TOTAL;
    }

    public Long getVolumeBandwidth() {
        return volumeBandwidth;
    }

    public void setVolumeBandwidth(Long volumeBandwidth) {
        this.volumeBandwidth = volumeBandwidth;
    }

    public Long getReadBandwidth() {
        return readBandwidth;
    }

    public void setReadBandwidth(Long readBandwidth) {
        this.readBandwidth = readBandwidth;
    }

    public Long getWriteBandwidth() {
        return writeBandwidth;
    }

    public void setWriteBandwidth(Long writeBandwidth) {
        this.writeBandwidth = writeBandwidth;
    }

    public Long getTotalBandwidth() {
        return totalBandwidth;
    }

    public void setTotalBandwidth(Long totalBandwidth) {
        this.totalBandwidth = totalBandwidth;
    }

    public Long getReadIOPS() {
        return readIOPS;
    }

    public void setReadIOPS(Long readIOPS) {
        this.readIOPS = readIOPS;
    }

    public Long getWriteIOPS() {
        return writeIOPS;
    }

    public void setWriteIOPS(Long writeIOPS) {
        this.writeIOPS = writeIOPS;
    }

    public Long getTotalIOPS() {
        return totalIOPS;
    }

    public void setTotalIOPS(Long totalIOPS) {
        this.totalIOPS = totalIOPS;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public static APISetVolumeQosMsg __example__() {
        APISetVolumeQosMsg msg = new APISetVolumeQosMsg();
        msg.setUuid(uuid());
        msg.setTotalBandwidth(10000L);
        msg.setTotalIOPS(1000L);
        return msg;
    }

}
