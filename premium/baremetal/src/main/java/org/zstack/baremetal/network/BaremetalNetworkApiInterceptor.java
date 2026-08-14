package org.zstack.baremetal.network;

import org.zstack.baremetal.BaremetalUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.baremetal.chassis.*;
import org.zstack.header.baremetal.network.APICreateBaremetalBondingMsg;
import org.zstack.header.baremetal.network.BaremetalBondingVO;
import org.zstack.header.baremetal.network.BaremetalBondingVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by GuoYi on 2019-01-03.
 */
@InterceptorForService("baremetal.network")
public class BaremetalNetworkApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(BaremetalNetworkApiInterceptor.class);

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateBaremetalBondingMsg) {
            validate((APICreateBaremetalBondingMsg) msg);
        }
        return msg;
    }

    private void validate(APICreateBaremetalBondingMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                BaremetalChassisStatus status = q(BaremetalChassisVO.class)
                        .eq(BaremetalChassisVO_.uuid, msg.getChassisUuid())
                        .select(BaremetalChassisVO_.status)
                        .findValue();
                if (status != BaremetalChassisStatus.Available) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "creating bm bonding is only allowed before creating bm instance"
                    ));
                }

                if (q(BaremetalBondingVO.class)
                        .eq(BaremetalBondingVO_.chassisUuid, msg.getChassisUuid())
                        .eq(BaremetalBondingVO_.name, msg.getName())
                        .isExists()) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "bond name %s already exists", msg.getName()
                    ));
                }

                msg.setSlaves(msg.getSlaves().toLowerCase());
                String[] slaves = msg.getSlaves().split(",");
                String nicInfo = q(BaremetalHardwareInfoVO.class)
                        .eq(BaremetalHardwareInfoVO_.chassisUuid, msg.getChassisUuid())
                        .eq(BaremetalHardwareInfoVO_.type, BaremetalChassisConstant.BAREMETAL_HARDWARE_INFO_NIC_TYPE)
                        .select(BaremetalHardwareInfoVO_.content)
                        .findValue();

                StringBuilder sb = new StringBuilder();
                long count = q(BaremetalBondingVO.class).count();
                sql("select bond from BaremetalBondingVO bond", BaremetalBondingVO.class)
                        .limit(1000)
                        .paginate(count, bonds -> bonds.forEach(bond -> {
                            BaremetalBondingVO vo = (BaremetalBondingVO) bond;
                            sb.append(vo.getSlaves());
                        }));
                String bondSlaves = sb.toString();

                for (String slave : slaves) {
                    if (!BaremetalUtils.isValidMacAddress(slave)) {
                        throw new ApiMessageInterceptionException(Platform.argerr(
                                "Slave address %s is invalid. It should be like 6c:b3:11:1b:0b:1e,6c:b3:11:1b:0b:1f", slave
                        ));
                    }

                    if (!nicInfo.contains(slave)) {
                        throw new ApiMessageInterceptionException(Platform.argerr(
                                "mac address %s does not belong to chassis[uuid:%s]", slave, msg.getChassisUuid()
                        ));
                    }

                    if (bondSlaves.contains(slave)) {
                        throw new ApiMessageInterceptionException(Platform.argerr(
                                "mac address %s is already a bond slave", slave
                        ));
                    }
                }
            }
        }.execute();
    }
}
