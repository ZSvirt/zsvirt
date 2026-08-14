package org.zstack.usbDevice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by GuoYi on 10/21/17.
 */
public class UsbDeviceCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(UsbDeviceCascadeExtension.class);
    private static final String NAME = UsbDeviceVO.class.getSimpleName();

    protected static final int OP_NOPE = 0;
    protected static final int OP_DETACH = 1;
    protected static final int OP_DELETION = 2;

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private CloudBus bus;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        int op = toDeleteOpCode(action);
        if (op == OP_NOPE) {
            completion.success();
            return;
        }

        final List<UsbDeviceInventory> inventories = getUsbDeviceFromAction(action);
        if (CollectionUtils.isEmpty(inventories)) {
            completion.success();
            return;
        }

        final List<String> uuidList = CollectionUtils.transform(inventories, UsbDeviceInventory::getUuid);
        switch (op) {
        case OP_DETACH:
            detachUsbFromVm(uuidList, completion);
            break;
        case OP_DELETION:
            deleteUsbFromDb(uuidList, completion);
            break;
        }
    }

    protected int toDeleteOpCode(CascadeAction action) {
        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DETACH;
        }

        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        return OP_NOPE;
    }

    private void detachUsbFromVm(List<String> usbUuidList, Completion completion) {
        new While<>(usbUuidList).step((uuid, cmpl) -> {
            DetachUsbDeviceMsg dmsg = new DetachUsbDeviceMsg();
            dmsg.setUsbDeviceUuid(uuid);

            bus.makeTargetServiceIdByResourceUuid(dmsg, UsbDeviceConstants.SERVICE_ID, uuid);
            bus.send(dmsg, new CloudBusCallBack(cmpl) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        logger.debug(String.format("successfully detached usb device[uuid:%s]", dmsg.getUsbDeviceUuid()));
                    } else {
                        logger.warn(reply.getError().toString());
                        UsbDeviceVO vo = Q.New(UsbDeviceVO.class).eq(UsbDeviceVO_.uuid, uuid).find();
                        vo.setVmInstanceUuid(null);
                        vo.setAttachType(null);
                        dbf.update(vo);
                        UsbSystemTags.USB_REDIRECT_PORT.delete(vo.getUuid());
                    }
                    cmpl.done();
                }
            });
        }, 3).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    private void deleteUsbFromDb(List<String> usbUuidList, Completion completion) {
        // delete UsbDeviceVO and related ResourceVO
        SQL.New(UsbDeviceVO.class)
                .in(UsbDeviceVO_.uuid, usbUuidList)
                .hardDelete();

        completion.success();
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(UsbDeviceVO.class);
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(
            VmInstanceVO.class.getSimpleName(),
            HostVO.class.getSimpleName()
        );
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<UsbDeviceInventory> ctx = getUsbDeviceFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }

    private List<UsbDeviceInventory> getUsbDeviceFromAction(CascadeAction action) {
        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VmDeletionStruct> structs = action.getParentIssuerContext();
            List<UsbDeviceVO> usbs = new ArrayList<>();
            for (VmDeletionStruct struct : structs) {
                VmInstanceInventory inv = struct.getInventory();
                usbs.addAll(Q.New(UsbDeviceVO.class).eq(UsbDeviceVO_.vmInstanceUuid, inv.getUuid()).list());
            }
            if (!usbs.isEmpty()) {
                return UsbDeviceInventory.valueOf(usbs);
            }
        } else if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            List<String> hostUuidList = CollectionUtils.transform(hosts, HostInventory::getUuid);
            List<UsbDeviceVO> usbList = Q.New(UsbDeviceVO.class)
                        .in(UsbDeviceVO_.hostUuid, hostUuidList)
                        .list();
            if (!usbList.isEmpty()) {
                return UsbDeviceInventory.valueOf(usbList);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }

        return null;
    }
}
