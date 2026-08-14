package org.zstack.zops;

import org.zstack.core.asyncbatch.While;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.zops.utils.Client;
import org.zstack.zops.utils.CommandResult;
import org.zstack.zops.utils.SshInfo;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.SshException;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public abstract class AbstractHostBackend {
    private static final CLogger logger = Utils.getLogger(AbstractHostBackend.class);
    protected String hostname = ZOpsConstants.UNKNOWN;
    protected Client client = new Client();
    protected Set<HostType> types = new HashSet<>();
    protected Set<String> extraIps = new HashSet<>();

    void updateChronyServers(List<String> chronyServers, Completion completion) {
        StringBuilder command = new StringBuilder();
        StringBuilder sourceConfig = new StringBuilder();
        String updateChronyConfigTemplate = "sudo bash -c 'echo \"%s\" >> /etc/chrony.conf'";

        chronyServers = chronyServers.stream().distinct().collect(Collectors.toList());
        for (String hostname : chronyServers) {
            sourceConfig.append(String.format("server %s iburst\n", hostname));
        }

        //TODO use scripts
        command.append("sudo sed -i /\"^[[:space:]#]*\\(server\\|pool\\)\"/d /etc/chrony.conf").append(" && ");

        if (!chronyServers.isEmpty()) {
            String updateChronyServerCmd = String.format(updateChronyConfigTemplate, sourceConfig.toString().replaceAll("\\n$", ""));
            command.append(updateChronyServerCmd).append(" && ");
        }

        String updateChronyAllowCmd = "(grep \"^allow\" -q /etc/chrony.conf >/dev/null 2>&1 ||" +
                String.format(updateChronyConfigTemplate, "allow 0.0.0.0/0") + ")";
        String updateChronyLocalStratumCmd = "(grep \"^local stratum \" -q /etc/chrony.conf >/dev/null 2>&1 ||" +
                String.format(updateChronyConfigTemplate, "local stratum 10") + ")";

        command.append(updateChronyAllowCmd).append(" && ")
                .append(updateChronyLocalStratumCmd).append(" && ")
                .append("sudo systemctl restart chronyd").append(" && ")
                .append("sudo hwclock -w");

        CommandResult res = client.command(command.toString()).run();
        if (res.getRetCode() != 0) {
            completion.fail(operr("failed to config time sources '%s' in %s, because:%s, raw: %s",
                    sourceConfig, hostname, res.getStderr(), res.getExecutionLog()));
            return;
        }
        logger.debug(String.format("successfully config time sources '%s' in %s", sourceConfig, hostname));
        completion.success();
    }

    void checkMultiHostsNetworkReachable(List<String> targetHostname, ReturnValueCompletion<List<NetworkReachablePair>> completion) {
        List<NetworkReachablePair> res = new ArrayList<>();
        for (String t : targetHostname) {
            NetworkReachablePair pair = new NetworkReachablePair();
            pair.setSourceHostname(hostname);
            pair.setTargetHostname(t);
            checkNetworkReachable(t, new Completion(null) {
                @Override
                public void success() {
                    pair.setStatus(HostConnectedStatus.Connected);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    pair.setStatus(HostConnectedStatus.Disconnected);
                }
            });
            res.add(pair);
        }
        completion.success(res);
    }

    void checkNetworkReachable(String targetHostname, Completion completion) {
        try {
            CommandResult res = client.command(String.format("timeout 1 ping %s -c 5 -i 0.01", targetHostname), true).run();
            if (res.getRetCode() != 0) {
                completion.fail(operr("%s is unreachable from %s, because:%s", targetHostname, hostname, res.getStderr()));
                return;
            }
            logger.debug(String.format("%s is reachable from %s", targetHostname, hostname));
            completion.success();
        } catch (SshException ex) {
            completion.fail(operr("failed to check is %s reachable from %s, because:%s", targetHostname, hostname, ex.getMessage()));
        }
    }

    void getCephMonHealthStatus(ReturnValueCompletion<String> completion) {
        try {
            String command = ZOpsManagerImpl.CHECK_CEPH_HEALTH_COMMAND;
            CommandResult res = client.command(command).run();

            if (!res.isSuccess()) {
                completion.fail(operr("%s failed to check ceph health status, because: %s", hostname,
                        res.getStderr()));
            }

            logger.debug(String.format("%s ceph health is: %s", hostname, res.getStdout()));

            String cephHealthResult = res.getStdout();
            completion.success(cephHealthResult);

        } catch (SshException ex) {
            completion.fail(operr("%s failed to check ceph health status, because: %s", hostname, ex.getMessage()));
        }
    }

    void getChronyServers(ReturnValueCompletion<List<ChronyServerInfoPair>> completion) {
        try {
            List<ChronyServerInfoPair> chronyServerStructPairs = new ArrayList<>();

            CommandResult res = client.command("awk '/^\\s*server/{print $2}' /etc/chrony.conf", true).run();
            //client.command("awk '/^\\s*server/{print $2}' /etc/chrony.conf");
            if (res.getRetCode() != 0) {
                completion.fail(operr("failed to get chrony sources, because:%s", res.getStderr()));
                return;
            }

            ChronyServerInfo internalHostStruct = new ChronyServerInfo();
            internalHostStruct.setHostname(hostname);
            internalHostStruct.setStatus(HostConnectedStatus.Connected);

            String[] sources = cleanOriginString(res.getStdout()).split("\n");
            logger.debug(String.format("read chrony server ip %s from configure file", Arrays.toString(sources)));
            if ((sources.length == 0 || "".equals(sources[0])) || (sources.length == 1 && sources[0].equals(hostname))) {
                ChronyServerInfoPair structPair = new ChronyServerInfoPair();
                structPair.setInternal(internalHostStruct);
                chronyServerStructPairs.add(structPair);
                completion.success(chronyServerStructPairs);
                return;
            }

            Set<String> chronySources = Arrays.stream(sources).filter(value -> !value.equals(hostname))
                    .collect(Collectors.toSet());;

            new While<>(chronySources).step((source, whileCompletion) -> {
                ChronyServerInfo externalHostStruct = new ChronyServerInfo();
                externalHostStruct.setHostname(source);
                checkNetworkReachable(source, new Completion(null) {
                    @Override
                    public void success() {
                        externalHostStruct.setStatus(HostConnectedStatus.Connected);
                        ChronyServerInfoPair structPair = new ChronyServerInfoPair();
                        structPair.setInternal(internalHostStruct);
                        structPair.setExternal(externalHostStruct);
                        chronyServerStructPairs.add(structPair);
                        whileCompletion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        externalHostStruct.setStatus(HostConnectedStatus.Disconnected);
                        ChronyServerInfoPair structPair = new ChronyServerInfoPair();
                        structPair.setInternal(internalHostStruct);
                        structPair.setExternal(externalHostStruct);
                        chronyServerStructPairs.add(structPair);
                        whileCompletion.done();
                    }
                });
            }, 4).run(new WhileDoneCompletion(completion) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    completion.success(chronyServerStructPairs);
                }
            });

        } catch (SshException ex) {
            completion.fail(operr("failed to get %s's chrony sources, because:%s", hostname, ex.getMessage()));
        }
    }

    void syncChronyServers(Completion completion) {
        String command = "sudo systemctl restart chronyd" + "&&" +
                "sudo chronyc -a makestep" + "&&" +
                "sudo hwclock -w";

        CommandResult res = client.command(command).run();
        if (res.getRetCode() != 0) {
            completion.fail(operr("failed to synchronize chrony server in %s, because:%s, raw: %s", hostname,
                    res.getStderr(), res.getExecutionLog()));
            return;
        }
        logger.debug(String.format("successfully synchronize chrony server in %s", hostname));
        completion.success();
    }

    static String cleanOriginString(String origin) {
        return origin.trim().replace("\r", "").replace(" ", "");
    }

    protected void setSshInfo(String hostname, int port, String privateKey) {
        SshInfo sshInfo = new SshInfo();
        sshInfo.setUsername("root");
        sshInfo.setPort(port);
        sshInfo.setHostname(hostname);
        sshInfo.setPrivateKey(privateKey);
        this.client.setSshInfo(sshInfo);
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getHostname() {
        return hostname;
    }

    public Set<HostType> getTypes() {
        return types;
    }

    public Set<String> getExtraIps() {
        return extraIps;
    }

    public Boolean isIpsExist(List<String> ips) {
        if (ips.contains(hostname)) return true;

        return ips.stream().anyMatch(extraIps::contains);
    }

    public Boolean isIpExist(String ip) {
        return ip.equals(hostname) || extraIps.contains(ip);
    }
}
