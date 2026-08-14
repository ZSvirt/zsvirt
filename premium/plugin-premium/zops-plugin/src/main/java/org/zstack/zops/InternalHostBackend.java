package org.zstack.zops;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.ansible.AnsibleFacade;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class InternalHostBackend extends AbstractHostBackend {
    @Autowired
    private AnsibleFacade asf;
    InternalHostBackend(String hostname, int port) {
        this.hostname = hostname;
        setSshInfo(hostname, port, asf.getPrivateKey());
        this.client.setHostname(hostname);
    }

    InternalHostBackend(HostBaseInfo hostBaseInfo) {
        this(hostBaseInfo.getHostname(), hostBaseInfo.getPort());
        extraIps.addAll(hostBaseInfo.getExtraIps());
        types.addAll(hostBaseInfo.getTypes());
    }
}
