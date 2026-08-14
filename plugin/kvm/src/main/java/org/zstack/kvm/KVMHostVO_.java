package org.zstack.kvm;

import org.zstack.header.host.HostVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(KVMHostVO.class)
public class KVMHostVO_ extends HostVO_ {
    public static volatile SingularAttribute<KVMHostVO, String> username;
    //cannot get password by using this method,because password is encrypted
    public static volatile SingularAttribute<KVMHostVO, String> password;
    public static volatile SingularAttribute<KVMHostVO, Integer> port;
    public static volatile SingularAttribute<KVMHostVO, String> osDistribution;
    public static volatile SingularAttribute<KVMHostVO, String> osRelease;
    public static volatile SingularAttribute<KVMHostVO, String> osVersion;
    public static volatile SingularAttribute<KVMHostVO, String> iscsiInitiatorName;
}
