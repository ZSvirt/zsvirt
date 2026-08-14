package org.zstack.header.simulator;

public class SimulatorDetails {
	private long memoryCapacity;
	private long cpuCapacity;
	
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
	
	public void fillAPIAddSimulatorHostMsg(APIAddSimulatorHostMsg msg) {
		msg.setCpuCapacity(getCpuCapacity());
		msg.setMemoryCapacity(getMemoryCapacity());
	}
}
