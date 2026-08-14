package org.zstack.usbDevice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.APIRevokeResourceSharingMsg;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.snapshot.group.MemorySnapshotValidatorExtensionPoint;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.identity.AccountManager;
import org.zstack.utils.CollectionDSL;

import javax.persistence.Tuple;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.i18n;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by GuoYi on 10/21/17.
 */
@InterceptorForService("usbDevice")
public class UsbDeviceApiInterceptor implements ApiMessageInterceptor, MemorySnapshotValidatorExtensionPoint, GlobalApiMessageInterceptor {
    public static List<VmInstanceState> allowedVmInstanceAttachableState = asList(VmInstanceState.Running, VmInstanceState.Stopped);
    public static List<VmInstanceState> allowedVmInstanceDetachableState = asList(VmInstanceState.Running, VmInstanceState.Stopped);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acmgr;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIQueryUsbDeviceMsg) {
            return msg;
        }

        if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
            return msg;
        }

        if (msg instanceof APIRevokeResourceSharingMsg) {
            validate((APIRevokeResourceSharingMsg) msg);
            return msg;
        }

        if (msg instanceof APIAttachUsbDeviceToVmMsg) {
            validate((APIAttachUsbDeviceToVmMsg) msg);
        } else if (msg instanceof APIDetachUsbDeviceFromVmMsg) {
            validate((APIDetachUsbDeviceFromVmMsg) msg);
        } else if (msg instanceof APIGetUsbDeviceCandidatesForAttachingVmMsg) {
            validate((APIGetUsbDeviceCandidatesForAttachingVmMsg) msg);
        } else if (msg instanceof APIUpdateUsbDeviceMsg) {
            validate((APIUpdateUsbDeviceMsg) msg);
        } 
        return msg;
    }

    private void validate(APICreateVmInstanceMsg msg) {
        if (isEmpty(msg.getSystemTags())) {
            return;
        }

        List<String> systemTags = filter(msg.getSystemTags(), VmSystemTags.VM_ATTACH_USB::isMatch);
        List<String> usbDeviceUuids = transform(systemTags,
                tag -> VmSystemTags.VM_ATTACH_USB.getTokenByTag(tag, VmSystemTags.USBDEVICE_UUID_TOKEN));

        if (usbDeviceUuids.isEmpty()) {
            return;
        }

        if (!acmgr.isAdmin(msg.getSession())) {
            String accountUuid = msg.getSession().getAccountUuid();
            final List<String> accessibleResources = acmgr.getResourceUuidsCanAccessByAccount(
                    accountUuid, UsbDeviceVO.class);
            final List<String> inaccessibleResources = filter(usbDeviceUuids, uuid -> !accessibleResources.contains(uuid));
            if (!inaccessibleResources.isEmpty()) {
                raiseUsbInaccessibleError(accountUuid, inaccessibleResources);
                return;
            }
        }

        if (Q.New(UsbDeviceVO.class).in(UsbDeviceVO_.uuid, usbDeviceUuids)
                .like(UsbDeviceVO_.usbVersion, "1%")
                .count() > UsbDeviceConstants.MAX_USB_1_DEVICE_PER_VM) {
            throw new ApiMessageInterceptionException(Platform.operr(
                    "You can attach at most %s USB 1.0 devices to one vm instance.",
                    UsbDeviceConstants.MAX_USB_1_DEVICE_PER_VM
            ));
        }

        if (Q.New(UsbDeviceVO.class).in(UsbDeviceVO_.uuid, usbDeviceUuids)
                .like(UsbDeviceVO_.usbVersion, "2%")
                .count() > UsbDeviceConstants.MAX_USB_2_DEVICE_PER_VM) {
            throw new ApiMessageInterceptionException(Platform.operr(
                    "You can attach at most %s USB 2.0 devices to one vm instance.",
                    UsbDeviceConstants.MAX_USB_2_DEVICE_PER_VM
            ));
        }

        if (Q.New(UsbDeviceVO.class).in(UsbDeviceVO_.uuid, usbDeviceUuids)
                .like(UsbDeviceVO_.usbVersion, "3%")
                .count() > UsbDeviceConstants.MAX_USB_3_DEVICE_PER_VM) {
            throw new ApiMessageInterceptionException(Platform.operr(
                    "You can attach at most %s USB 3.0 devices to one vm instance.",
                    UsbDeviceConstants.MAX_USB_1_DEVICE_PER_VM
            ));
        }
    }

    private void validate(APIAttachUsbDeviceToVmMsg msg) {
        UsbDeviceVO usb = dbf.findByUuid(msg.getUsbDeviceUuid(), UsbDeviceVO.class);

        if (msg.getVmInstanceUuid().equals(usb.getVmInstanceUuid())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the usb device[uuid:%s] has already been attached to same vm[uuid:%s]",
                    msg.getUsbDeviceUuid(), usb.getVmInstanceUuid()
            ));
        }

        if (usb.getVmInstanceUuid() != null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the usb device[uuid:%s] has already been attached to another vm[uuid:%s]",
                    msg.getUsbDeviceUuid(), usb.getVmInstanceUuid()
            ));
        }

        if (usb.getState() != UsbDeviceState.Enabled) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the usb device[uuid:%s] is not in attachable state of %s",
                    msg.getUsbDeviceUuid(), UsbDeviceState.Enabled
            ));
        }

        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (!(allowedVmInstanceAttachableState.contains(vm.getState()))) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the vm instance[uuid:%s] is not in attachable state of %s for usb device",
                    msg.getVmInstanceUuid(), allowedVmInstanceAttachableState
            ));
        }

        //3.5 vm can attach usb by redirect
        //detectUsbDeviceHostConflict(usb, vm);
        HostVO host = dbf.findByUuid(usb.getHostUuid(), HostVO.class);
        if (!host.getState().equals(HostState.Enabled) || !host.getStatus().equals(HostStatus.Connected)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the host that the usb device[uuid:%s] pluged in is not in valid state[%s] or status[%s]",
                    usb.getHostUuid(), msg.getUsbDeviceUuid(), HostState.Enabled, HostStatus.Connected
            ));
        }
    }

    private void detectUsbDeviceHostConflict(UsbDeviceVO usb, VmInstanceVO vm) {
        List<UsbDeviceVO> attached = Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.vmInstanceUuid, vm.getUuid())
                .list();
        for (UsbDeviceVO vo : attached) {
            if (!vo.getHostUuid().equals(usb.getHostUuid())) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "the usb device[uuid:%s] has different hostUuid than devices that already attached to the vm instance[uuid:%s]",
                        usb.getUuid(), vm.getUuid()
                ));
            }
        }
    }

    private void validate(APIDetachUsbDeviceFromVmMsg msg) {
        UsbDeviceVO usb = dbf.findByUuid(msg.getUsbDeviceUuid(), UsbDeviceVO.class);

        if (usb.getVmInstanceUuid() == null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the usb device[uuid:%s] is not attached to any vm instance.",
                    usb.getUuid()
            ));
        }

        VmInstanceVO vm = dbf.findByUuid(usb.getVmInstanceUuid(), VmInstanceVO.class);
        if (!(allowedVmInstanceDetachableState.contains(vm.getState()))) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the vm instance that the usb device[uuid:%s] is attached to is not in detachable state of %s",
                    usb.getUuid(), allowedVmInstanceDetachableState
            ));
        }
    }

    private void validate(APIGetUsbDeviceCandidatesForAttachingVmMsg msg) {
        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (!(allowedVmInstanceAttachableState.contains(vm.getState()))) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "vm instance[uuid:%s] not in attachable state of %s for usb device",
                    vm.getUuid(),
                    allowedVmInstanceAttachableState
            ));
        }
    }

    private void validate(APIUpdateUsbDeviceMsg msg) {
        UsbDeviceVO usb = dbf.findByUuid(msg.getUuid(), UsbDeviceVO.class);
        if (usb.getVmInstanceUuid() != null && msg.getState() != null && msg.getState().equals("Disabled")) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot disable usb device[uuid:%s] when it's attached to a vm instance",
                    msg.getUuid()
            ));
        }
    }

    private void validate(APIRevokeResourceSharingMsg msg) {
        final List<String> resourceUuids = msg.getResourceUuids();
        if (isEmpty(resourceUuids)) {
            return;
        }

        List<Tuple> tuples = Q.New(UsbDeviceVO.class)
                .in(UsbDeviceVO_.uuid, resourceUuids)
                .notNull(UsbDeviceVO_.vmInstanceUuid)
                .select(UsbDeviceVO_.uuid, UsbDeviceVO_.vmInstanceUuid, UsbDeviceVO_.hostUuid)
                .listTuple();
        if (tuples.isEmpty()) {
            return;
        }

        List<UsbOccupiedStruct> structs = transform(tuples, UsbOccupiedStruct::ofUsbVmTuple);
        Set<String> vmInstanceUuidSet = transformToSet(tuples, tuple -> tuple.get(1, String.class));
        List<Tuple> vmAccountTuples = Q.New(AccountResourceRefVO.class)
                .select(AccountResourceRefVO_.resourceUuid, AccountResourceRefVO_.accountUuid)
                .in(AccountResourceRefVO_.resourceUuid, vmInstanceUuidSet)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .listTuple();

        for (Tuple vmAccountTuple : vmAccountTuples) {
            final String vmInstanceUuid = vmAccountTuple.get(0, String.class);
            final String accountUuid = vmAccountTuple.get(1, String.class);

            final List<UsbOccupiedStruct> filterStructs =
                    filter(structs, struct -> struct.vmInstanceUuid.equals(vmInstanceUuid));
            filterStructs.forEach(struct -> struct.accountUuid = accountUuid);
        }

        structs.removeIf(struct -> struct.accountUuid.equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID));
        if (structs.isEmpty()) {
            return; // all vm are belong to admin
        }

        if (msg.isAll()) {
            raiseUsbOccupiedError(structs);
        }

        if (isEmpty(msg.getAccountUuids())) {
            return;
        }

        structs.removeIf(struct -> !msg.getAccountUuids().contains(struct.accountUuid));
        if (!isEmpty(structs)) {
            raiseUsbOccupiedError(structs);
        }
    }

    private void raiseUsbOccupiedError(List<UsbOccupiedStruct> structs) throws ApiMessageInterceptionException {
        Set<String> uuidSet = structs.stream()
                .map(struct -> asList(struct.usbDeviceUuid, struct.hostUuid, struct.vmInstanceUuid, struct.accountUuid))
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        Map<String, String> uuidNameMap = Q.New(ResourceVO.class)
                .select(ResourceVO_.uuid, ResourceVO_.resourceName)
                .in(ResourceVO_.uuid, uuidSet)
                .listTuple()
                .stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, String.class), tuple -> tuple.get(1, String.class)));
        for (UsbOccupiedStruct struct : structs) {
            struct.usbName = uuidNameMap.getOrDefault(struct.usbDeviceUuid, "unknown");
            struct.vmName = uuidNameMap.getOrDefault(struct.vmInstanceUuid, "unknown");
            struct.hostName = uuidNameMap.getOrDefault(struct.hostUuid, "unknown");
            struct.accountName = uuidNameMap.getOrDefault(struct.accountUuid, "unknown");
        }

        throw new ApiMessageInterceptionException(Platform.argerr("%s",
                String.join("; ", transform(structs, UsbOccupiedStruct::i18n))
        ));
    }

    private void raiseUsbInaccessibleError(String accountUuid, List<String> usbDeviceUuidList) throws ApiMessageInterceptionException {
        final List<Tuple> tuples = Q.New(ResourceVO.class)
                .select(ResourceVO_.uuid, ResourceVO_.resourceName)
                .in(ResourceVO_.uuid, usbDeviceUuidList)
                .listTuple();
        final List<String> scripts = transform(tuples,
                tuple -> i18n("[uuid:%s, name:%s]", tuple.get(0, String.class), tuple.get(1, String.class)));
        String arrayText = String.join(", ", scripts);

        throw new ApiMessageInterceptionException(Platform.err(SysErrors.RESOURCE_NOT_ACCESSIBLE,
                "the usb devices%s is inaccessible for account[uuid:%s]", arrayText, accountUuid));
    }

    @Override
    public ErrorCode checkVmWhereMemorySnapshotExistExternalDevices(String VmInstanceUuid) {
        if (Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.vmInstanceUuid, VmInstanceUuid)
                .isExists()) {
            return argerr("please umount all usb devices of the vm[%s] and try again", VmInstanceUuid);
        }
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return CollectionDSL.list(
                APICreateVmInstanceMsg.class,
                APIRevokeResourceSharingMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    private static class UsbOccupiedStruct {
        String usbDeviceUuid;
        String vmInstanceUuid;
        String hostUuid;
        String accountUuid;

        String usbName;
        String vmName;
        String hostName;
        String accountName;

        static UsbOccupiedStruct ofUsbVmTuple(Tuple tuple) {
            UsbOccupiedStruct struct = new UsbOccupiedStruct();
            struct.usbDeviceUuid = tuple.get(0, String.class);
            struct.vmInstanceUuid = tuple.get(1, String.class);
            struct.hostUuid = tuple.get(2, String.class);
            return struct;
        }

        String i18n() {
            return Platform.i18n(
                    "the usb devices[uuid:%s, name:%s] in host[uuid:%s, name:%s] is occupied by account[uuid:%s, name:%s] and vm[uuid:%s, name:%s]",
                    usbDeviceUuid, usbName, hostUuid, hostName, accountUuid, accountName, vmInstanceUuid, vmName
            );
        }
    }
}
