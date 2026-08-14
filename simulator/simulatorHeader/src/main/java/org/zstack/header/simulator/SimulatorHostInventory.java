package org.zstack.header.simulator;

import org.zstack.header.host.HostInventory;
import org.zstack.header.search.Inventory;

@Inventory(mappingVOClass = SimulatorHostVO.class)
public class SimulatorHostInventory extends HostInventory {
	private Long memoryCapacity;
	private Long cpuCapacity;
	
	protected SimulatorHostInventory(SimulatorHostVO vo) {
		super(vo);
		this.memoryCapacity = vo.getMemoryCapacity();
		this.cpuCapacity = vo.getCpuCapacity();
	}
	
	public SimulatorHostInventory() {
	}
	
	public static SimulatorHostInventory valueOf(SimulatorHostVO vo) {
		SimulatorHostInventory inv = new SimulatorHostInventory(vo);
		return inv;
	}
	
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
}
