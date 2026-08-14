package org.zstack.header.simulator.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;

@RestRequest(
        path = "/primary-storage/simulators",
        method = HttpMethod.POST,
        responseClass = APIAddPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIAddSimulatorPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
	private long totalCapacity = 100000000;
    private long availableCapacity = 10000000;
    private long availablePhysicalCapacity = 10000000;
    private long totalPhysicalCapacity = 10000000;
	
	public APIAddSimulatorPrimaryStorageMsg() {
	}

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getTotalCapacity() {
    	return totalCapacity;
    }
	public void setTotalCapacity(long totalCapacity) {
    	this.totalCapacity = totalCapacity;
    }


    public long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }

    public long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }

    public static APIAddSimulatorPrimaryStorageMsg __example__() {
        APIAddSimulatorPrimaryStorageMsg msg = new APIAddSimulatorPrimaryStorageMsg();
        msg.setUrl("/simulator");
        msg.setName("simulator");
        msg.setZoneUuid(uuid());
        return msg;
    }

}
