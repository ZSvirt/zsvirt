package org.zstack.compute.host;


import java.util.Map;

public interface HostResourceAllocationStrategyInterface {
   void allocate(int vCpuNum, long memSize, boolean cyclicDistribution);
}
