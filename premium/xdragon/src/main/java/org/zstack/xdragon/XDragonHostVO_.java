package org.zstack.xdragon;

import org.zstack.kvm.KVMHostVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(XDragonHostVO.class)
public class XDragonHostVO_ extends KVMHostVO_ {
    public static volatile SingularAttribute<XDragonHostVO, Integer> cpuNum;
    public static volatile SingularAttribute<XDragonHostVO, Integer> cpuSockets;
    public static volatile SingularAttribute<XDragonHostVO, Long> totalPhysicalMemory;
}
