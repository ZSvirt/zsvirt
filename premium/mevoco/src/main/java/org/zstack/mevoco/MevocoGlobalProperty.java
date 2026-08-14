package org.zstack.mevoco;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class MevocoGlobalProperty {
    @GlobalProperty(name="ui_mode", defaultValue = "zstack")
    public static String UI_MODE;
    @GlobalProperty(name="Spice.certs", defaultValue = "/var/lib/zstack/kvm/package/spice-certs/ca-cert.pem")
    public static String REGISTRY_CERTS;
    @GlobalProperty(name="qemu.conf", defaultValue = "/etc/libvirt/qemu.conf")
    public static String QEMU_CONF;
    @GlobalProperty(name="deploy_mode", defaultValue = "zsv")
    public static String DEPLOY_MODE;
}
