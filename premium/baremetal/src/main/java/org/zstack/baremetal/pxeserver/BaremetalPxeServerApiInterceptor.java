package org.zstack.baremetal.pxeserver;

import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.baremetal.BaremetalUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.baremetal.BaremetalConstant;
import org.zstack.header.baremetal.instance.BaremetalInstanceVO;
import org.zstack.header.baremetal.instance.BaremetalInstanceVO_;
import org.zstack.header.baremetal.network.BaremetalNicVO;
import org.zstack.header.baremetal.network.BaremetalNicVO_;
import org.zstack.header.baremetal.pxeserver.*;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.AccountManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 * Created by GuoYi on 2017/3/28.
 */
@InterceptorForService("baremetal.pxeserver")
public class BaremetalPxeServerApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    protected AccountManager acmgr;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateBaremetalPxeServerMsg) {
            validate((APICreateBaremetalPxeServerMsg) msg);
        } else if (msg instanceof APIUpdateBaremetalPxeServerMsg) {
            validate((APIUpdateBaremetalPxeServerMsg) msg);
        } else if (msg instanceof APIAttachBaremetalPxeServerToClusterMsg) {
            validate((APIAttachBaremetalPxeServerToClusterMsg) msg);
        } else if (msg instanceof APIDetachBaremetalPxeServerFromClusterMsg) {
            validate((APIDetachBaremetalPxeServerFromClusterMsg) msg);
        }
        return msg;
    }

    private void validatePxeServer(String begin, String end, String netmask) {
        DebugUtils.Assert(begin != null && end != null && netmask != null &&
                !begin.trim().isEmpty() && !end.trim().isEmpty() && !netmask.trim().isEmpty(),
                "dhcp range begin/end/netmask cannot be null or empty"
        );

        try {
            NetworkUtils.validateIpRange(begin, end);
        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(Platform.argerr(e.getMessage()));
        }

        if (!NetworkUtils.isNetmask(netmask)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "PXE Server DHCP Range Netmask %s is invalid.", netmask
            ));
        }

        if (!BaremetalUtils.isBelongToSameSubnet(begin, end, netmask)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "PXE Server DHCP Range Start %s and Range Stop %s do not belong to the same subnet.", begin, end
            ));
        }
    }

    private String getSshPassCommand(APICreateBaremetalPxeServerMsg msg) {
        return String.format(
                "timeout 10 sshpass -p '%s' ssh -q -o UserKnownHostsFile=/dev/null -o PubkeyAuthentication=no -o StrictHostKeyChecking=no -p %d %s@%s PATH=/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/sbin",
                msg.getSshPassword(),
                msg.getSshPort(),
                msg.getSshUsername(),
                msg.getHostname());
    }

    private void validate(APICreateBaremetalPxeServerMsg msg) {
        if (Q.New(BaremetalPxeServerVO.class).eq(BaremetalPxeServerVO_.hostname, msg.getHostname()).isExists()) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "PXE Server with hostname %s already exists.",
                    msg.getHostname()
            ));
        }

        if (!msg.getStoragePath().startsWith("/")) {
            throw new ApiMessageInterceptionException(Platform.argerr("storagePath should be an absolute path"));
        }

        // check local repo, dhcp interface and dhcp range etc.
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            msg.setDhcpRangeBegin(msg.getDhcpRangeBegin() == null ? "10.0.0.100" : msg.getDhcpRangeBegin());
            msg.setDhcpRangeEnd(msg.getDhcpRangeEnd() == null ? "10.0.0.200" : msg.getDhcpRangeEnd());
            msg.setDhcpRangeNetmask(msg.getDhcpRangeNetmask() == null ? "255.255.255.0" : msg.getDhcpRangeNetmask());
            msg.setDhcpInterfaceAddress("10.0.0.10");
        } else {
            String sshPassCmd = getSshPassCommand(msg);

            ShellResult rst = ShellUtils.runAndReturn(String.format(
                    "%s true", sshPassCmd
            ));
            if (rst.getRetCode() != 0) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "failed to connect to %s, please check network connection between zstack management node and baremetal pxeserver",
                        msg.getHostname()
                ));
            }

            rst = ShellUtils.runAndReturn(String.format(
                    "%s \'[ \"$(ls -A /opt/zstack-dvd)\" ]\' 2>/dev/null", sshPassCmd
            ));
            if (rst.getRetCode() != 0) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "no local repo found under /opt/zstack-dvd of %s, please download zstack iso and create local repo first", msg.getHostname()
                ));
            }

            rst = ShellUtils.runAndReturn(String.format(
                    "%s ip addr show %s 2>/dev/null | grep -w inet | head -n 1 | awk '{ print $2 }'",
                    sshPassCmd, msg.getDhcpInterface()
            ));
            if (rst.getStdout().isEmpty() || rst.getStdout().split("/").length != 2) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "PXE Server DHCP Interface %s does not exists, or it does not have an IP address.", msg.getDhcpInterface()
                ));
            }

            String cidr = rst.getStdout().trim();
            if (!NetworkUtils.isCidr(cidr) ||
                    (msg.getDhcpRangeBegin() != null && !NetworkUtils.isIpv4InCidr(msg.getDhcpRangeBegin(), cidr)) ||
                    (msg.getDhcpRangeEnd() != null && !NetworkUtils.isIpv4InCidr(msg.getDhcpRangeEnd(), cidr))) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "%s ~ %s cannot connect to dhcp interface %s",
                        msg.getDhcpRangeBegin(), msg.getDhcpRangeEnd(), msg.getDhcpInterface()
                ));
            }

            SubnetUtils sub = new SubnetUtils(cidr);
            sub.setInclusiveHostCount(false);
            msg.setDhcpRangeBegin(msg.getDhcpRangeBegin() == null ? sub.getInfo().getLowAddress() : msg.getDhcpRangeBegin());
            msg.setDhcpRangeEnd(msg.getDhcpRangeEnd() == null ? sub.getInfo().getHighAddress() : msg.getDhcpRangeEnd());
            msg.setDhcpRangeNetmask(msg.getDhcpRangeNetmask() == null ? BaremetalUtils.lengthToNetmask(cidr.split("/")[1]) : msg.getDhcpRangeNetmask());
            msg.setDhcpInterfaceAddress(cidr.split("/")[0]);
        }

        validatePxeServer(
                msg.getDhcpRangeBegin(),
                msg.getDhcpRangeEnd(),
                msg.getDhcpRangeNetmask()
        );
    }

    private void validate(APIUpdateBaremetalPxeServerMsg msg) {
        BaremetalPxeServerVO vo = dbf.findByUuid(msg.getUuid(), BaremetalPxeServerVO.class);

        // combine user input and database
        String rangeBegin, rangeEnd, rangeMask;
        rangeBegin = msg.getDhcpRangeBegin() == null ? vo.getDhcpRangeBegin() : msg.getDhcpRangeBegin();
        rangeEnd = msg.getDhcpRangeEnd() == null ? vo.getDhcpRangeEnd() : msg.getDhcpRangeEnd();
        rangeMask = msg.getDhcpRangeNetmask() == null ? vo.getDhcpRangeNetmask() : msg.getDhcpRangeNetmask();
        validatePxeServer(rangeBegin, rangeEnd, rangeMask);
    }

    private void validate(APIAttachBaremetalPxeServerToClusterMsg msg) {
        ClusterVO cls = dbf.findByUuid(msg.getClusterUuid(), ClusterVO.class);
        BaremetalPxeServerVO pxe = dbf.findByUuid(msg.getPxeServerUuid(), BaremetalPxeServerVO.class);
        // check zone uuid
        if (!cls.getZoneUuid().equals(pxe.getZoneUuid())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cluster[uuid:%s] and pxeserver[uuid:%s] don't belong to one zone",
                    msg.getClusterUuid(), msg.getPxeServerUuid()
            ));
        }

        // check cluster type
        if (!cls.getType().equalsIgnoreCase(BaremetalConstant.BAREMETAL_CLUSTER_TYPE) ||
                !cls.getHypervisorType().equalsIgnoreCase(BaremetalConstant.BAREMETAL_HYPERVISOR_TYPE)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cluster[uuid:%s] is not baremetal cluster",
                    msg.getClusterUuid()
            ));
        }

        // check already attached
        if (Q.New(BaremetalPxeServerClusterRefVO.class)
                .eq(BaremetalPxeServerClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(BaremetalPxeServerClusterRefVO_.pxeServerUuid, msg.getPxeServerUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "baremetal pxeserver[uuid:%s] already attached to cluster[uuid:%s]",
                    msg.getPxeServerUuid(), msg.getClusterUuid()
            ));
        }

        // check baremetal instance that already exists
        new SQLBatch() {
            @Override
            protected void scripts() {
                List<String> bmUuids = Q.New(BaremetalInstanceVO.class)
                        .select(BaremetalInstanceVO_.uuid)
                        .eq(BaremetalInstanceVO_.clusterUuid, msg.getClusterUuid())
                        .listValues();
                if (bmUuids != null && !bmUuids.isEmpty()) {
                    List<String> ips = Q.New(BaremetalNicVO.class)
                            .in(BaremetalNicVO_.baremetalInstanceUuid, bmUuids)
                            .eq(BaremetalNicVO_.pxe, true)
                            .select(BaremetalNicVO_.ip)
                            .listValues();

                    BaremetalPxeServerVO pxe = dbf.findByUuid(msg.getPxeServerUuid(), BaremetalPxeServerVO.class);
                    String begin = pxe.getDhcpRangeBegin();
                    String end = pxe.getDhcpRangeEnd();
                    for (String ip : ips) {
                        if (!NetworkUtils.isIpv4InRange(ip, begin, end)) {
                            throw new ApiMessageInterceptionException(Platform.argerr(
                                    "baremetal pxeserver[uuid:%s] is not compatible with baremetal instances in cluster[uuid:%s], " +
                                            "existing nic ip %s is out of pxeserver dhcp range %s ~ %s.",
                                    msg.getPxeServerUuid(), msg.getClusterUuid(), ip, begin, end
                            ));
                        }
                    }
                }
            }
        }.execute();
    }

    private void validate(APIDetachBaremetalPxeServerFromClusterMsg msg) {
        if (!Q.New(BaremetalPxeServerClusterRefVO.class)
                .eq(BaremetalPxeServerClusterRefVO_.pxeServerUuid, msg.getPxeServerUuid())
                .eq(BaremetalPxeServerClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(argerr(
                    "baremetal pxeserver[uuid: %s] not attached to cluster[uuid: %s]",
                    msg.getPxeServerUuid(), msg.getClusterUuid()
            ));
        }
    }
}
