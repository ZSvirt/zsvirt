
package org.zstack.header.volume.block;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.storage.volume.block.BlockVolumeMessage;
import org.zstack.storage.volume.block.XskyBlockVolumeMessage;

@RestRequest(
        path = "/xsky/block-volumes/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateBlockVolumeEvent.class,
        isAction = true
)
public class APIUpdateXskyBlockVolumeMsg extends APIUpdateBlockVolumeMsg implements XskyBlockVolumeMessage {
    @APIParam(required = false)
    private Long burstTotalBw;

    @APIParam(required = false)
    private Long burstTotalIops;

    @APIParam(required = false)
    private Long maxTotalBw;

    @APIParam(required = false)
    private Long maxTotalIops;

    public Long getBurstTotalBw() {
        return burstTotalBw;
    }

    public void setBurstTotalBw(Long burstTotalBw) {
        this.burstTotalBw = burstTotalBw;
    }

    public Long getBurstTotalIops() {
        return burstTotalIops;
    }

    public void setBurstTotalIops(Long burstTotalIops) {
        this.burstTotalIops = burstTotalIops;
    }

    public Long getMaxTotalBw() {
        return maxTotalBw;
    }

    public void setMaxTotalBw(Long maxTotalBw) {
        this.maxTotalBw = maxTotalBw;
    }

    public Long getMaxTotalIops() {
        return maxTotalIops;
    }

    public void setMaxTotalIops(Long maxTotalIops) {
        this.maxTotalIops = maxTotalIops;
    }

    public static APIUpdateXskyBlockVolumeMsg __example__() {
        APIUpdateXskyBlockVolumeMsg msg = new APIUpdateXskyBlockVolumeMsg();
        msg.setUuid(uuid());
        msg.setName("example");
        return msg;
    }
}
