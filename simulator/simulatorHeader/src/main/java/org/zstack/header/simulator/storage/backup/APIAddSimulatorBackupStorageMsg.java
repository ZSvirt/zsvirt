package org.zstack.header.simulator.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.header.storage.backup.APIAddBackupStorageMsg;


@RestRequest(
        path = "/backup-storage/simulators",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddBackupStorageEvent.class
)
public class APIAddSimulatorBackupStorageMsg extends APIAddBackupStorageMsg {
	private long totalCapacity;
	private long availableCapacity;
	
	public APIAddSimulatorBackupStorageMsg() {
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
 
    public static APIAddSimulatorBackupStorageMsg __example__() {
        APIAddSimulatorBackupStorageMsg msg = new APIAddSimulatorBackupStorageMsg();
        msg.setUrl("/simulator");
        msg.setName("simulator-backup-storage");
        return msg;
    }

}
