package org.zstack.baremetal.chassis;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.baremetal.chassis.*;
import org.zstack.header.baremetal.network.BaremetalNicInfoStruct;
import org.zstack.header.baremetal.pxeserver.*;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created by GuoYi on 2018-10-30.
 */
public class BaremetalChassisExtensionToPxeServer implements BaremetalPxeServerAttachExtensionPoint, BaremetalPxeServerDetachExtensionPoint {
    private static final CLogger logger = Utils.getLogger(BaremetalChassisExtensionToPxeServer.class);

    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    protected CloudBus bus;

    @Override
    public void preAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void beforeAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void failToAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void afterAttachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {
        // recreate dhcp configs in pxeserver
        List<CreateBaremetalDhcpConfigMsg> cmsgs = new SQLBatchWithReturn<List<CreateBaremetalDhcpConfigMsg>>() {
            @Override
            protected List<CreateBaremetalDhcpConfigMsg> scripts() {
                // only set pxeServerUuid of chassis that are not HWInfoUnknown
                sql(BaremetalChassisVO.class)
                        .eq(BaremetalChassisVO_.clusterUuid, clusterUuid)
                        .notEq(BaremetalChassisVO_.status, BaremetalChassisStatus.HWInfoUnknown)
                        .set(BaremetalChassisVO_.pxeServerUuid, inventory.getUuid())
                        .update();

                List<CreateBaremetalDhcpConfigMsg> cmsgs = new ArrayList<>();
                List<String> chassisUuids = q(BaremetalChassisVO.class)
                        .eq(BaremetalChassisVO_.clusterUuid, clusterUuid)
                        .eq(BaremetalChassisVO_.pxeServerUuid, inventory.getUuid())
                        .select(BaremetalChassisVO_.uuid)
                        .listValues();
                if (chassisUuids.isEmpty()) {
                    return cmsgs;
                }

                List<Tuple> tuples = q(BaremetalHardwareInfoVO.class)
                        .select(BaremetalHardwareInfoVO_.chassisUuid, BaremetalHardwareInfoVO_.content)
                        .eq(BaremetalHardwareInfoVO_.type, BaremetalChassisConstant.BAREMETAL_HARDWARE_INFO_NIC_TYPE)
                        .in(BaremetalHardwareInfoVO_.chassisUuid, chassisUuids)
                        .listTuple();
                if (tuples.isEmpty()) {
                    return cmsgs;
                }

                Type listType = new TypeToken<ArrayList<BaremetalNicInfoStruct>>(){}.getType();
                for (Tuple tuple : tuples) {
                    List<BaremetalNicInfoStruct> structs = new Gson().fromJson(tuple.get(1, String.class), listType);
                    Optional<BaremetalNicInfoStruct> struct = structs.stream().filter(BaremetalNicInfoStruct::getPxe).findFirst();
                    if (!struct.isPresent()) {
                        logger.error(String.format("hardware info of pxe boot nic of chassis[uuid:%s] not found", tuple.get(0, String.class)));
                        continue;
                    }

                    CreateBaremetalDhcpConfigMsg cmsg = new CreateBaremetalDhcpConfigMsg();
                    cmsg.setPxeServerUuid(inventory.getUuid());
                    cmsg.setChassisUuid(tuple.get(0, String.class));
                    cmsg.setPxeNicMac(struct.get().getMac());
                    cmsg.setPxeNicIp(struct.get().getIp());
                    bus.makeTargetServiceIdByResourceUuid(cmsg, BaremetalPxeServerConstant.SERVICE_ID, inventory.getUuid());
                    cmsgs.add(cmsg);
                }

                return cmsgs;
            }
        }.execute();

        if (cmsgs.isEmpty()) {
            return;
        }

        new While<>(cmsgs).all((cmsg, whileCompletion) -> bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.error(reply.getError().getDetails());
                }
                whileCompletion.done();
            }
        })).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                logger.debug(String.format(
                        "recreated dhcp configs on pxeserver[uuid:%s] for bm chassis in cluster[uuid:%s]",
                        inventory.getUuid(), clusterUuid
                ));
            }
        });
    }

    @Override
    public void preDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void beforeDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void failToDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {

    }

    @Override
    public void afterDetachBaremetalPxeServer(BaremetalPxeServerInventory inventory, String clusterUuid) {
        // delete dhcp configs in pxeserver
        List<DeleteBaremetalDhcpConfigMsg> dmsgs = new SQLBatchWithReturn<List<DeleteBaremetalDhcpConfigMsg>>() {
            @Override
            protected List<DeleteBaremetalDhcpConfigMsg> scripts() {
                List<DeleteBaremetalDhcpConfigMsg> dmsgs = new ArrayList<>();
                List<String> chassisUuids = q(BaremetalChassisVO.class)
                        .eq(BaremetalChassisVO_.clusterUuid, clusterUuid)
                        .eq(BaremetalChassisVO_.pxeServerUuid, inventory.getUuid())
                        .select(BaremetalChassisVO_.uuid)
                        .listValues();
                if (chassisUuids.isEmpty()) {
                    return dmsgs;
                }

                // set pxeServerUuid of all releated baremetal chassis to null
                sql(BaremetalChassisVO.class)
                        .eq(BaremetalChassisVO_.clusterUuid, clusterUuid)
                        .eq(BaremetalChassisVO_.pxeServerUuid, inventory.getUuid())
                        .set(BaremetalChassisVO_.pxeServerUuid, null)
                        .update();

                dmsgs = transform(chassisUuids, chassisUuid -> {
                    DeleteBaremetalDhcpConfigMsg dmsg = new DeleteBaremetalDhcpConfigMsg();
                    dmsg.setPxeServerUuid(inventory.getUuid());
                    dmsg.setChassisUuid(chassisUuid);
                    bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, inventory.getUuid());
                    return dmsg;
                });
                return dmsgs;
            }
        }.execute();

        if (dmsgs.isEmpty()) {
            return;
        }

        new While<>(dmsgs).all((dmsg, whileCompletion) -> bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.error(reply.getError().getDetails());
                }
                whileCompletion.done();
            }
        })).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                logger.debug(String.format(
                        "deleted dhcp configs on pxeserver[uuid:%s] for bm chassis in cluster[uuid:%s]",
                        clusterUuid, inventory.getUuid()
                ));
            }
        });
    }
}
