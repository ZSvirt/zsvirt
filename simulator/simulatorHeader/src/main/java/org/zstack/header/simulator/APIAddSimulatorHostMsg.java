package org.zstack.header.simulator;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.host.APIAddHostMsg;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
		path = "/hosts/simulators",
		method = HttpMethod.POST,
		parameterName = "params",
		responseClass = APIAddHostEvent.class
)
public class APIAddSimulatorHostMsg extends APIAddHostMsg {
	@APIParam
	private long memoryCapacity = 1000000000;
	@APIParam
	private long cpuCapacity = 1000000000;
	
	public long getMemoryCapacity() {
    	return memoryCapacity;
    }
	public void setMemoryCapacity(long memoryCapacity) {
    	this.memoryCapacity = memoryCapacity;
    }
	public long getCpuCapacity() {
    	return cpuCapacity;
    }
	public void setCpuCapacity(long cpuCapacity) {
    	this.cpuCapacity = cpuCapacity;
    }

 
    public static APIAddSimulatorHostMsg __example__() {
        APIAddSimulatorHostMsg msg = new APIAddSimulatorHostMsg();
        msg.setName("simulator");
        msg.setManagementIp("127.0.0.1");
        msg.setClusterUuid(uuid());
        return msg;
    }

}
