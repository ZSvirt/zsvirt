package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.concurrent.TimeUnit;

/**
 * @api live migrate vm to another host
 * @cli
 * @httpMsg {
 * "org.zstack.header.vm.APIMigrateVmMsg": {
 * "vmInstanceUuid": "7fed8055cc004d8fbafd2918efdb2fcc",
 * "hostUuid": "4796eba438884c49b6d754b33f84d582",
 * "session": {
 * "uuid": "f16d128c42eb4e90b0490499c4e51e67"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.vm.APIMigrateVmMsg": {
 * "vmInstanceUuid": "7fed8055cc004d8fbafd2918efdb2fcc",
 * "hostUuid": "4796eba438884c49b6d754b33f84d582",
 * "session": {
 * "uuid": "f16d128c42eb4e90b0490499c4e51e67"
 * },
 * "timeout": 1800000,
 * "id": "9fab8a857fe941beaaef2d5d2c30cb50",
 * "serviceId": "api.portal"
 * }
 * }
 * @result See :ref:`APIMigrateVmEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/actions",
        isAction = true,
        responseClass = APIMigrateVmEvent.class,
        method = HttpMethod.PUT
)
@SkipVmTracer(replyClass = APIMigrateVmEvent.class)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 1)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIMigrateVmMsg extends APIMessage implements VmInstanceMessage, MigrateVmMessage, CheckAttachedVolumesMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;
    /**
     * @desc uuid of host the vm is migrating to. If omitted, zstack will try to find
     * an available host itself
     * @optional
     */
    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;

    /**
     * @desc set true mean migration is called from destination host
     */
    @APIParam(required = false)
    private Boolean migrateFromDestination;

    @APIParam(required = false)
    private boolean allowUnknown;

    @APIParam(required = false, validValues = {"auto-converge"})
    private String strategy;

    @APIParam(required = false)
    private Integer downTime;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmUuid() {
        return vmInstanceUuid;
    }

    public void setVmUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public boolean isMigrateFromDestination() {
        return migrateFromDestination == null ? false : migrateFromDestination;
    }

    public void setMigrateFromDestination(Boolean migrateFromDestination) {
        this.migrateFromDestination = migrateFromDestination;
    }

    public boolean isAllowUnknown() {
        return allowUnknown;
    }

    public void setAllowUnknown(boolean allowUnknown) {
        this.allowUnknown = allowUnknown;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    @Override
    public String getVmInstanceUuid() {
        return getVmUuid();
    }

    public Integer getDownTime() {
        return downTime;
    }

    public void setDownTime(Integer downTime) {
        this.downTime = downTime;
    }

    public static APIMigrateVmMsg __example__() {
        APIMigrateVmMsg msg = new APIMigrateVmMsg();
        msg.vmInstanceUuid = uuid();
        msg.hostUuid = uuid();
        msg.setMigrateFromDestination(false);
        msg.setDownTime(300);
        return msg;
    }

}
