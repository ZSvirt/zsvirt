package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmHostnameUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.upgrade.UpgradeGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.agent.ProxyHardware;
import org.zstack.header.agent.ProxyHardwareFactory;
import org.zstack.header.agent.versioncontrol.AgentVersionVO;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class HostApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof HostMessage) {
            HostMessage hmsg = (HostMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);

        if (msg instanceof APIAddHostMsg) {
            validate((APIAddHostMsg) msg);
        } else if (msg instanceof APIUpdateHostMsg) {
            validate((APIUpdateHostMsg) msg);
        } else if (msg instanceof APIDeleteHostMsg) {
            validate((APIDeleteHostMsg) msg);
        } else if (msg instanceof APIChangeHostStateMsg){
            validate((APIChangeHostStateMsg) msg);
        } else if (msg instanceof APIReconnectHostMsg){
            validate((APIReconnectHostMsg) msg);
        } else if (msg instanceof APIGetHostWebSshUrlMsg) {
            validate((APIGetHostWebSshUrlMsg) msg);
        } else if (msg instanceof APIGetPhysicalMachineBlockDevicesMsg) {
            validate((APIGetPhysicalMachineBlockDevicesMsg) msg);
        } else if (msg instanceof APIMountBlockDeviceMsg) {
            validate((APIMountBlockDeviceMsg) msg);
        } else if (msg instanceof APIUpdateHostnameMsg) {
            validate((APIUpdateHostnameMsg) msg);
        }

        return msg;
    }

    private void validate(APIGetHostWebSshUrlMsg msg) {
        String ZOPS_CONTAINER_NAME = "zops-controller";
        ShellResult ret;
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            ret = ShellUtils.runAndReturn(String.format("docker exec %s systemctl is-active webssh", ZOPS_CONTAINER_NAME));
        } else {
            ret = new ShellResult();
            ret.setDesensitizeCmd(String.format("docker exec %s systemctl is-active webssh", ZOPS_CONTAINER_NAME));
            ret.setRetCode(0);
        }
        if (!ret.isReturnCode(0)) {
            throw new ApiMessageInterceptionException(operr("webssh server is not running."));
        }
    }

    private void validate(APIDeleteHostMsg msg) {
        if (!dbf.isExist(msg.getUuid(), HostVO.class)) {
            APIDeleteHostEvent evt = new APIDeleteHostEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIUpdateHostMsg msg) {
        if (msg.getManagementIp() != null) {
            SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
            q.add(HostVO_.managementIp, Op.EQ, msg.getManagementIp());
            if (q.isExists()) {
                throw new ApiMessageInterceptionException(argerr("there has been a host having managementIp[%s]", msg.getManagementIp()));
            }
        }
    }

    private void validate(APIAddHostMsg msg) {
        if (!NetworkUtils.isIpv4Address(msg.getManagementIp()) && !NetworkUtils.isHostname(msg.getManagementIp())) {
            throw new ApiMessageInterceptionException(argerr("managementIp[%s] is neither an IPv4 address nor a valid hostname", msg.getManagementIp()));
        }
    }

    private void validate(APIChangeHostStateMsg msg){
        HostStatus hostStatus = Q.New(HostVO.class)
                .select(HostVO_.status)
                .eq(HostVO_.uuid,msg.getHostUuid())
                .findValue();
        if (hostStatus != HostStatus.Connected && msg.getStateEvent().equals(HostStateEvent.maintain.toString())){
            throw new ApiMessageInterceptionException(operr("can not maintain host[uuid:%s, status:%s]which is not Connected", msg.getHostUuid(), hostStatus));
        }
    }

    private void validate(APIReconnectHostMsg msg) {
        String hostUuid = msg.getHostUuid();
        if (UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)) {
            AgentVersionVO agentVersionVO = dbf.findByUuid(msg.getUuid(), AgentVersionVO.class);
            if (agentVersionVO == null) {
                agentVersionVO = new AgentVersionVO();
                agentVersionVO.setUuid(hostUuid);
                agentVersionVO.setAgentType("kvm-agent");
                dbf.persist(agentVersionVO);
            }
        }
    }

    private void validate(APIGetPhysicalMachineBlockDevicesMsg msg) {
        if (msg.getPassword() != null) {
            return;
        }
        ProxyHardware proxyHardware = getProxyHardware(msg.getHostName());
        if (proxyHardware == null) {
            throw new ApiMessageInterceptionException(operr("the password for the physical machine [%s] is empty. " +
                    "please set a password", msg.getHostName()));
        }
        msg.setPassword(proxyHardware.getPassword());
        msg.setUsername(msg.getUsername() != null ? msg.getUsername() : proxyHardware.getUsername());
    }

    private void validate(APIMountBlockDeviceMsg msg) {
        if (msg.getPassword() != null) {
            return;
        }
        ProxyHardware proxyHardware = getProxyHardware(msg.getHostName());
        if (proxyHardware == null) {
            throw new ApiMessageInterceptionException(operr("the password for the physical machine [%s] is empty. " +
                    "please set a password", msg.getHostName()));
        }
        msg.setPassword(proxyHardware.getPassword());
        msg.setUsername(msg.getUsername() != null ? msg.getUsername() : proxyHardware.getUsername());

        validatePath(msg.getPath());
        validateMountPoint(msg.getMountPoint());
    }

    private void validate(APIUpdateHostnameMsg msg) {
        VmHostnameUtils.validateHostname(msg.getHostname(), false);
    }

    private static final String SAFE_MOUNT_POINT_PATTERN = "^[a-zA-Z0-9_\\-./]+$";
    private static final String SAFE_DEVICE_PATH_PATTERN = "^[a-zA-Z0-9_\\-./:=+]+$";

    private void validateAbsolutePathCommon(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("%s cannot be empty", fieldName));
        }
        if (!value.startsWith("/")) {
            throw new ApiMessageInterceptionException(operr(
                    "%s must be an absolute path (start with '/')", fieldName));
        }
        if (value.contains("..") || value.contains("//")) {
            throw new ApiMessageInterceptionException(operr(
                    "path traversal detected in %s", fieldName));
        }
    }

    private void validatePath(String path) {
        validateAbsolutePathCommon(path, "device path");
        if (!path.matches(SAFE_DEVICE_PATH_PATTERN)) {
            throw new ApiMessageInterceptionException(operr(
                    "device path must match pattern '%s'. " +
                            "allowed characters: alphanumeric, '-', '_', '.', '/', ':', '=', '+' " +
                            "(':' and '=' are needed for /dev/disk/by-path/pci-0000:00:1f.2-ata-1 " +
                            "and some LVM mapper names). " +
                            "valid examples: /dev/sda, /dev/mapper/mpath0, " +
                            "/dev/disk/by-path/pci-0000:00:1f.2-ata-1, /dev/disk/by-id/wwn-0x5000cca00a123456. " +
                            "shell metacharacters are rejected to prevent command injection. " +
                            "invalid value detected: '%s'",
                    SAFE_DEVICE_PATH_PATTERN, path));
        }
    }

    private void validateMountPoint(String mountPoint) {
        validateAbsolutePathCommon(mountPoint, "mount point");
        if (!mountPoint.matches(SAFE_MOUNT_POINT_PATTERN)) {
            throw new ApiMessageInterceptionException(operr(
                    "mount point must match pattern '%s'. " +
                            "allowed characters: alphanumeric, '-', '_', '.', '/'. " +
                            "valid examples: /mnt/data, /volumes/drive01, /backup-2023.disk. " +
                            "shell metacharacters are rejected to prevent command injection. " +
                            "invalid value detected: '%s'",
                    SAFE_MOUNT_POINT_PATTERN, mountPoint));
        }
        if (mountPoint.endsWith("/") && !mountPoint.equals("/")) {
            throw new ApiMessageInterceptionException(operr(
                    "mountPoint should not end with '/' except root directory"));
        }
    }

    private ProxyHardware getProxyHardware(String hostname) {
        for (ProxyHardwareFactory factory : pluginRgty.getExtensionList(ProxyHardwareFactory.class)) {
            ProxyHardware proxyHardware = factory.getProxyHardware(hostname);
            if (proxyHardware != null && proxyHardware.getPassword() != null) {
                return proxyHardware;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
            APIAddHostMsg.class,
            APIChangeHostStateMsg.class,
            APIDeleteHostMsg.class,
            APIReconnectHostMsg.class,
            APIQueryHostMsg.class,
            APIGetHypervisorTypesMsg.class,
            APIUpdateHostMsg.class,
            APIGetHostTaskMsg.class,
            APIUpdateHostIpmiMsg.class,
            APIShutdownHostMsg.class,
            APIPowerOnHostMsg.class,
            APIPowerResetHostMsg.class,
            APIGetHostWebSshUrlMsg.class,
            APIGetHostPowerStatusMsg.class,
            APIGetPhysicalMachineBlockDevicesMsg.class,
            APIMountBlockDeviceMsg.class,
            APIGetHostBlockDevicesMsg.class,
            APIGetHostSensorsMsg.class,
            APIUpdateHostNqnMsg.class,
            APIUpdateHostnameMsg.class
        );
    }

    private static List<Class> loadOptionalApiClasses(String... names) {
        List<Class> result = new ArrayList<>(names.length);
        for (String name : names) {
            try {
                result.add(Class.forName(name));
            } catch (ClassNotFoundException ignored) {}
        }
        return result;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.DEFAULT;
    }
}
