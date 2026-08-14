package org.zstack.header.simulator;

public interface SimulatorConnection {
	String getHostUuid();
	
	long getTotalMemory();

	long getTotalCpu();

	long getUsedMemory();

	long getUsedCpu();
}
