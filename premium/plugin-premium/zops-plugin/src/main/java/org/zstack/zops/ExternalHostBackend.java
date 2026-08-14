package org.zstack.zops;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zops.utils.Client;
import org.zstack.zops.utils.CommandResult;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ExternalHostBackend extends AbstractHostBackend {
    private static final CLogger logger = Utils.getLogger(AbstractHostBackend.class);

    ExternalHostBackend(String hostname) {
        this.hostname = hostname;
    }

    @Override
    void checkMultiHostsNetworkReachable(List<String> targetHostname, ReturnValueCompletion<List<NetworkReachablePair>> completion) {
        List<NetworkReachablePair> res = new ArrayList<>();
        for (String t : targetHostname) {
            NetworkReachablePair pair = new NetworkReachablePair();
            pair.setSourceHostname(hostname);
            pair.setTargetHostname(t);
            pair.setStatus(HostConnectedStatus.Unknown);
            res.add(pair);
        }

        completion.success(res);
    }

    @Override
    public void checkNetworkReachable(String targetHostname, Completion completion) {
        completion.fail(operr("fail to check is %s reachable from host %s, because %s is not managed by us", targetHostname, hostname, hostname));
    }

    @Override
    public void getCephMonHealthStatus(ReturnValueCompletion<String> completion) {
        completion.success("UNKNOWN");
    }

    @Override
    public void getChronyServers(ReturnValueCompletion<List<ChronyServerInfoPair>> completion) {
        ChronyServerInfo externalServerStruct = new ChronyServerInfo();
        externalServerStruct.setHostname(hostname);
        List<ChronyServerInfoPair> chronyServerStructPairs = new ArrayList<>();

        //TODO: make local mn to a bean.
        CommandResult res = new Client().runLocalCommand(String.format("timeout 1 ping %s -c 5 -i 0.01", hostname), true);
        ChronyServerInfoPair structPair = new ChronyServerInfoPair();

        if (res.getRetCode() != 0) {
            externalServerStruct.setStatus(HostConnectedStatus.Disconnected);
        } else {
            externalServerStruct.setStatus(HostConnectedStatus.Connected);
        }

        structPair.setExternal(externalServerStruct);
        chronyServerStructPairs.add(structPair);
        completion.success(chronyServerStructPairs);
    }
}
