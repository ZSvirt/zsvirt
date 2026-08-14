package org.zstack.baremetal.chassis;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.baremetal.BaremetalUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.baremetal.BaremetalConstant;
import org.zstack.header.baremetal.chassis.*;
import org.zstack.header.baremetal.pxeserver.*;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.AccountManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.List;


/**
 * Created by GuoYi on 2017/3/28.
 */
@InterceptorForService("baremetal.chassis")
public class BaremetalChassisApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(BaremetalChassisApiInterceptor.class);

    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    protected AccountManager acmgr;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateBaremetalChassisMsg) {
            validate((APICreateBaremetalChassisMsg) msg);
        } else if (msg instanceof APIUpdateBaremetalChassisMsg) {
            validate((APIUpdateBaremetalChassisMsg) msg);
        } else if (msg instanceof APIPowerOffBaremetalChassisMsg) {
            validate((APIPowerOffBaremetalChassisMsg) msg);
        } else if (msg instanceof APIPowerResetBaremetalChassisMsg) {
            validate((APIPowerResetBaremetalChassisMsg) msg);
        } else if (msg instanceof APIInspectBaremetalChassisMsg) {
            validate((APIInspectBaremetalChassisMsg) msg);
        }
        return msg;
    }

    private void validate(String address, Integer port, String username, String password) {
        if (!NetworkUtils.isIpv4Address(address)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "IPMI Address %s is not valid", address
            ));
        }

        if (!BaremetalUtils.isIpmiServerReachable(address, port, username, password)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Failed to reach the bare-metal chassis, please make sure: " +
                            "1. the IPMI connection is active; " +
                            "2. the IPMI Address, Port, Username and Password are correct; " +
                            "3. IPMI Over LAN is enabled in BIOS."
            ));
        }
    }

    public ErrorCode validate(CreateBaremetalChassisMsg msg) {
        String address = msg.getIpmiAddress();
        Integer port = msg.getIpmiPort();

        boolean chassisExists = Q.New(BaremetalChassisVO.class)
                .eq(BaremetalChassisVO_.ipmiAddress, address)
                .eq(BaremetalChassisVO_.ipmiPort, port)
                .isExists();
        if (chassisExists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Baremetal Chassis of IPMI address %s and IPMI port %d has already been created.", address, port
            ));
        }

        String username = msg.getIpmiUsername();
        String password = msg.getIpmiPassword();
        validate(address, port, username, password);

        // check the cluster
        ClusterVO cluster = dbf.findByUuid(msg.getClusterUuid(), ClusterVO.class);
        if (cluster == null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Cluster[uuid:%s] does not exists.",
                    msg.getClusterUuid()
            ));
        }
        if (!cluster.getType().equals(BaremetalConstant.BAREMETAL_CLUSTER_TYPE) ||
                !cluster.getHypervisorType().equals(BaremetalConstant.BAREMETAL_HYPERVISOR_TYPE)) {
            return Platform.argerr(
                    "Cluster[uuid:%s] is not a baremetal cluster.",
                    msg.getClusterUuid()
            );
        }
        if (cluster.getState() != ClusterState.Enabled) {
            return Platform.argerr(
                    "Cluster[uuid:%s] is not Enabled.",
                    msg.getClusterUuid()
            );
        }

        if (msg.getReboot()) {
            checkPxeServerBeforeInspectChassis(msg.getClusterUuid());
        }

        return null;
    }

    private void validate(APICreateBaremetalChassisMsg msg) {
        CreateBaremetalChassisMsg cmsg = CreateBaremetalChassisMsg.valueOf(msg);
        ErrorCode err = validate(cmsg);
        if (err != null) {
            throw new ApiMessageInterceptionException(err);
        }
    }

    private void validate(APIUpdateBaremetalChassisMsg msg) {
        String uuid = msg.getUuid();
        BaremetalChassisVO chassis = dbf.findByUuid(uuid, BaremetalChassisVO.class);

        String address = msg.getIpmiAddress() != null ? msg.getIpmiAddress() : chassis.getIpmiAddress();
        Integer port = msg.getIpmiPort() != null ? msg.getIpmiPort() : chassis.getIpmiPort();
        String username = msg.getIpmiUsername() != null ? msg.getIpmiUsername() : chassis.getIpmiUsername();
        String password = msg.getIpmiPassword() != null ? msg.getIpmiPassword() : chassis.getIpmiPassword();
        validate(address, port, username, password);

        // address:port should not be same as other chassis
        boolean chassisExists = Q.New(BaremetalChassisVO.class)
                .eq(BaremetalChassisVO_.ipmiAddress, address)
                .eq(BaremetalChassisVO_.ipmiPort, port)
                .notEq(BaremetalChassisVO_.uuid, uuid)
                .isExists();
        if (chassisExists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "IPMI Address and Port %s:%d already exists.", address, port
            ));
        }
    }

    private void validate(APIPowerOffBaremetalChassisMsg msg) {
        /*
        BaremetalChassisVO chassis = dbf.findByUuid(msg.getChassisUuid(), BaremetalChassisVO.class);
        if (chassis.getStatus() == BaremetalChassisStatus.Allocated) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Baremetal chassis[uuid:%s] has already been allocated, do not poweroff.",
                    msg.getChassisUuid()
            ));
        }
        */
    }

    private void validate(APIPowerResetBaremetalChassisMsg msg) {
        /*
        BaremetalChassisVO chassis = dbf.findByUuid(msg.getChassisUuid(), BaremetalChassisVO.class);
        if (chassis.getStatus() == BaremetalChassisStatus.Allocated) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Baremetal chassis[uuid:%s] has already been allocated, do not reboot it.",
                    msg.getChassisUuid()
            ));
        }
        */
    }

    private void validate(APIInspectBaremetalChassisMsg msg) {
        BaremetalChassisVO chassis = dbf.findByUuid(msg.getUuid(), BaremetalChassisVO.class);
        /*
        if (chassis.getStatus() == BaremetalChassisStatus.Allocated) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Baremetal chassis[uuid:%s] has already been allocated, do not reboot it.",
                    msg.getUuid()
            ));
        }
        */

        checkPxeServerBeforeInspectChassis(chassis.getClusterUuid());
    }

    private void checkPxeServerBeforeInspectChassis(String clusterUuid) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                List<String> uuids = q(BaremetalPxeServerClusterRefVO.class)
                        .eq(BaremetalPxeServerClusterRefVO_.clusterUuid, clusterUuid)
                        .select(BaremetalPxeServerClusterRefVO_.pxeServerUuid)
                        .listValues();
                if (!uuids.isEmpty()) {
                    List<Tuple> tuples = q(BaremetalPxeServerVO.class)
                            .in(BaremetalPxeServerVO_.uuid, uuids)
                            .select(BaremetalPxeServerVO_.state, BaremetalPxeServerVO_.status)
                            .listTuple();
                    for (Tuple tuple : tuples) {
                        if (tuple.get(0, BaremetalPxeServerState.class) == BaremetalPxeServerState.Enabled &&
                                tuple.get(1, BaremetalPxeServerStatus.class) == BaremetalPxeServerStatus.Connected) {
                            return;
                        }
                    }
                }

                throw new ApiMessageInterceptionException(Platform.argerr(
                        "no usable baremetal pxeserver attached to cluster[uuid:%s]",
                        clusterUuid
                ));
            }
        }.execute();
    }
}
