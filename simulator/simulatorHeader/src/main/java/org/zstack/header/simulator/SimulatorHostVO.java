package org.zstack.header.simulator;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vo.EO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = HostEO.class, needView = false)
public class SimulatorHostVO extends HostVO {
	@Column
	private long memoryCapacity;
	
	@Column
	private long cpuCapacity;

	public SimulatorHostVO(){
	}
	
	public SimulatorHostVO(HostVO vo) {
	    super(vo);
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
