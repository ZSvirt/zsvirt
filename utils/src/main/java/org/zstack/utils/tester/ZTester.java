package org.zstack.utils.tester;

/**
 * Created by mingjian.deng on 2019/1/3.
 */
public interface ZTester {
    Object get(String key);
    <T> T get(String key, T defaultValue, Class<T> clazz);
    void set(String key, Object value);
    void setNull(String key);
    void clearAll();
    void clear(String key);

    String NULL_Flag = "null.for.test";

    String KVM_HostVersion = "kvm.host.version";
    String KVM_LibvirtVersion = "kvm.libvirt.version";
    String KVM_QemuImageVersion = "kvm.qemu.image.version";
    String KVM_CpuModelName = "kvm.cpu.model.name";
    String KVM_CpuProcessorNum = "kvm.cpu.processor.num";
    String KVM_IpmiAddress = "kvm.ipmi.address";
}
