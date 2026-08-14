package org.zstack.baremetal.instance;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.baremetal.BaremetalUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.baremetal.chassis.*;
import org.zstack.header.baremetal.instance.*;
import org.zstack.header.baremetal.network.BaremetalBondingVO;
import org.zstack.header.baremetal.network.BaremetalBondingVO_;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerState;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerStatus;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.identity.AccountManager;

import java.util.*;

/**
 * Created by GuoYi on 7/5/18.
 */
@InterceptorForService("baremetal.instance")
public class BaremetalInstanceApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    protected AccountManager acmgr;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateBaremetalInstanceMsg) {
            validate((APICreateBaremetalInstanceMsg) msg);
        } else if (msg instanceof APIRecoverBaremetalInstanceMsg) {
            validate((APIRecoverBaremetalInstanceMsg) msg);
        } else if (msg instanceof APIExpungeBaremetalInstanceMsg) {
            validate((APIExpungeBaremetalInstanceMsg) msg);
        }
        return msg;
    }

    private void validate(APICreateBaremetalInstanceMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                // check baremetal chassis
                BaremetalChassisVO chassis = findByUuid(msg.getChassisUuid(), BaremetalChassisVO.class);
                if (chassis == null) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "Baremetal chassis[uuid:%s] does not exist", msg.getChassisUuid()
                    ));
                }
                if (chassis.getState() != BaremetalChassisState.Enabled ||
                        chassis.getStatus() != BaremetalChassisStatus.Available) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "Baremetal chassis[uuid:%s] is not Enabled or Available, please choose another one.",
                            chassis.getUuid()
                    ));
                }
                if (chassis.getPxeServerUuid() == null) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "no corresponding pxeserver, please inspect baremetal chassis[uuid:%s] again",
                            chassis.getUuid()
                    ));
                }

                // check chassis hardware info
                String nicInfo = q(BaremetalHardwareInfoVO.class)
                        .eq(BaremetalHardwareInfoVO_.chassisUuid, chassis.getUuid())
                        .eq(BaremetalHardwareInfoVO_.type, BaremetalChassisConstant.BAREMETAL_HARDWARE_INFO_NIC_TYPE)
                        .select(BaremetalHardwareInfoVO_.content)
                        .findValue();
                if (chassis.getStatus() == BaremetalChassisStatus.HWInfoUnknown || nicInfo == null || nicInfo.equals("")) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "No hardware info found for baremetal chassis[uuid:%s], please choose another one.",
                            chassis.getUuid()
                    ));
                }
                msg.setNicInfo(nicInfo);

                // check pxeserver
                BaremetalPxeServerVO pxe = findByUuid(chassis.getPxeServerUuid(), BaremetalPxeServerVO.class);
                if (pxe == null || pxe.getState() != BaremetalPxeServerState.Enabled
                        || pxe.getStatus() != BaremetalPxeServerStatus.Connected) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "baremetal pxeserver[uuid:%s] is neither Enabled nor Connected, please check",
                            chassis.getPxeServerUuid()
                    ));
                }

                List<String> l3Uuids = new ArrayList<>();
                String clusterUuid = q(BaremetalChassisVO.class)
                        .eq(BaremetalChassisVO_.uuid, chassis.getUuid())
                        .select(BaremetalChassisVO_.clusterUuid)
                        .findValue();

                // check network configuration
                if (msg.getNicCfgs() != null && !msg.getNicCfgs().isEmpty()) {
                    for (Map.Entry<String, String> entry : msg.getNicCfgs().entrySet()) {
                        String mac = entry.getKey();
                        if (!BaremetalUtils.isValidMacAddress(mac)) {
                            throw new ApiMessageInterceptionException(Platform.argerr(
                                    "Mac address %s is invalid. It should be like 6c:b3:11:1b:0b:1e", mac
                            ));
                        }

                        if (!nicInfo.contains(mac.toLowerCase())) {
                            throw new ApiMessageInterceptionException(Platform.argerr(
                                    "Baremetal chassis[uuid:%s] doesn't have nic with mac address %s",
                                    chassis.getUuid(), mac
                            ));
                        }

                        l3Uuids.add(entry.getValue());
                    }
                } else {
                    msg.setNicCfgs(new HashMap<>());
                }

                // check bonding configuration
                if (msg.getBondingCfgs() != null && !msg.getBondingCfgs().isEmpty()) {
                    List<String> bondingUuids = new ArrayList<>(msg.getBondingCfgs().keySet());
                    l3Uuids.addAll(msg.getBondingCfgs().values());

                    if (bondingUuids.size() != msg.getBondingCfgs().size()) {
                        throw new ApiMessageInterceptionException(Platform.argerr("duplicated bm bonding uuid detacted"));
                    }

                    if (q(BaremetalBondingVO.class).in(BaremetalBondingVO_.uuid, bondingUuids).count() != bondingUuids.size()) {
                        throw new ApiMessageInterceptionException(Platform.argerr("Baremetal Bonding does not exist"));
                    }
                } else {
                    msg.setBondingCfgs(new HashMap<>());
                }

                if (!l3Uuids.isEmpty()) {
                    if (l3Uuids.size() != new HashSet<>(l3Uuids).size()) {
                        throw new ApiMessageInterceptionException(Platform.argerr("duplicated l3 network uuid detacted"));
                    }

                    if (q(L3NetworkVO.class).in(L3NetworkVO_.uuid, l3Uuids).count() != l3Uuids.size()) {
                        throw new ApiMessageInterceptionException(Platform.argerr("the selected l3 network doesn't exist"));
                    }

                    // check l2 network attached to bm cluster
                    Set<String> l2Uuids = new HashSet<>(q(L3NetworkVO.class)
                            .in(L3NetworkVO_.uuid, l3Uuids)
                            .select(L3NetworkVO_.l2NetworkUuid)
                            .listValues());
                    boolean inCluster = q(L2NetworkClusterRefVO.class)
                            .eq(L2NetworkClusterRefVO_.clusterUuid, clusterUuid)
                            .in(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuids)
                            .count() == l2Uuids.size();
                    if (!inCluster) {
                        throw new ApiMessageInterceptionException(Platform.argerr(
                                "the selected l3 network cannot be assigned to chassis[uuid:%s]", chassis.getUuid()
                        ));
                    }
                }

                // check image
                ImageConstant.ImageMediaType type = q(ImageVO.class)
                        .eq(ImageVO_.uuid, msg.getImageUuid())
                        .select(ImageVO_.mediaType)
                        .findValue();
                if (type != ImageConstant.ImageMediaType.ISO) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "only iso image is supported in zstack baremetal service"
                    ));
                }

                String bsType = sql("select bs.type from BackupStorageVO bs, ImageBackupStorageRefVO ref " +
                        "where ref.imageUuid = :imageUuid and ref.backupStorageUuid = bs.uuid group by bs.uuid", String.class)
                        .param("imageUuid", msg.getImageUuid()).find();
                if (!bsType.equals("ImageStoreBackupStorage")) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "only ImageStoreBackupStorage is supported in zstack baremetal service"
                    ));
                }

                // check platform
                ImagePlatform platform = msg.getPlatform() == null ?
                        q(ImageVO.class)
                                .eq(ImageVO_.uuid, msg.getImageUuid())
                                .select(ImageVO_.platform)
                                .findValue() :
                        ImagePlatform.valueOf(msg.getPlatform());
                if (platform == null) {
                    throw new ApiMessageInterceptionException(Platform.argerr("image platform cannot be null")
                            .withOpaque("params", "platform"));
                }
                msg.setPlatform(platform.toString());
            }
        }.execute();
    }

    private void validate(APIRecoverBaremetalInstanceMsg msg) {
        BaremetalInstanceState state = Q.New(BaremetalInstanceVO.class)
                .eq(BaremetalInstanceVO_.uuid, msg.getUuid())
                .select(BaremetalInstanceVO_.state)
                .findValue();
        if (state != BaremetalInstanceState.Destroyed) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot recover baremetal instance that's not in Destroyed state"
            ));
        }
    }

    private void validate(APIExpungeBaremetalInstanceMsg msg) {
        BaremetalInstanceState state = Q.New(BaremetalInstanceVO.class)
                .eq(BaremetalInstanceVO_.uuid, msg.getUuid())
                .select(BaremetalInstanceVO_.state)
                .findValue();
        if (state != BaremetalInstanceState.Destroyed) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot expunge baremetal instance that's not in Destroyed state"
            ));
        }
    }
}
