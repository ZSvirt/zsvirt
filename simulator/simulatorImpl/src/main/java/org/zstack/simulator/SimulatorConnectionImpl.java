package org.zstack.simulator;

import org.zstack.header.simulator.SimulatorConnection;

class SimulatorConnectionImpl implements SimulatorConnection {
	private final SimulatorHost host;
	
	SimulatorConnectionImpl(SimulatorHost host) {
		this.host = host;
	}

	@Override
    public String getHostUuid() {
	    return host.getSimulatorHostVO().getUuid();
    }

	@Override
    public long getTotalMemory() {
	    return host.getSimulatorHostVO().getMemoryCapacity();
    }

	@Override
    public long getTotalCpu() {
	    return host.getSimulatorHostVO().getCpuCapacity();
    }

	@Override
    public long getUsedMemory() {
	    return 0;
    }

	@Override
    public long getUsedCpu() {
	    return 0;
    }
}
