package org.zstack.storage.primary.block;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.primary.block.message.DeleteBlockPrimaryStorageHostMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/7/19 15:41
 */
public class BlockPrimaryStorageHostInitiatorCascadeExtension extends AbstractAsyncCascadeExtension {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = BlockPrimaryStorageHostRefVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            completion.success();
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletionCleanup(CascadeAction action, final Completion completion) {
        completion.success();
    }

    private void handleDeletion(CascadeAction action, final Completion completion) {

        final List<BlockPrimaryStorageHostRefVO> blockPrimaryStorageHostRefVOList = initiatorFromAction(action);
        List<String> primaryStorageUuidList = primaryStorageUuidsFromAction(action);
        if (blockPrimaryStorageHostRefVOList == null || blockPrimaryStorageHostRefVOList.isEmpty() || primaryStorageUuidList == null) {
            completion.success();
            return;
        }

        List<DeleteBlockPrimaryStorageHostMsg> msgs = new ArrayList<DeleteBlockPrimaryStorageHostMsg>();
        primaryStorageUuidList.forEach(primaryStorageUuid -> {
            for (BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO : blockPrimaryStorageHostRefVOList) {
                DeleteBlockPrimaryStorageHostMsg msg = new DeleteBlockPrimaryStorageHostMsg();
                msg.setHostUuid(blockPrimaryStorageHostRefVO.getHostUuid());
                msg.setMetadata(blockPrimaryStorageHostRefVO.getMetadata());
                msg.setPrimaryStorageUuid(primaryStorageUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, BlockPrimaryStorageConstants.SERVICE_ID, blockPrimaryStorageHostRefVO.getHostUuid());
                msgs.add(msg);
            }
        });

        bus.send(msgs, 1, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                    for (MessageReply reply : replies) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }
                    }
                }
                completion.success();
            }
        });
    }

    private List<String> primaryStorageUuidsFromAction(CascadeAction action) {
        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            List<String> clusterUuid = hosts.stream().map(HostInventory::getClusterUuid).collect(Collectors.toList());
            List<String> primaryUuids = Q.New(PrimaryStorageClusterRefVO.class)
                    .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                    .in(PrimaryStorageClusterRefVO_.clusterUuid, clusterUuid)
                    .listValues();
            return  primaryUuids;
        }
        return null;
    }

    private List<BlockPrimaryStorageHostRefVO> initiatorFromAction(CascadeAction action) {
        List<BlockPrimaryStorageHostRefVO> blockPrimaryStorageHostRefVOList = null;
        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            List<String> hostUuids = transformAndRemoveNull(hosts, HostInventory::getUuid);
            blockPrimaryStorageHostRefVOList = Q.New(BlockPrimaryStorageHostRefVO.class)
                    .in(BlockPrimaryStorageHostRefVO_.hostUuid, hostUuids)
                    .list();
        } else if (NAME.equals(action.getParentIssuer())) {
            blockPrimaryStorageHostRefVOList = action.getParentIssuerContext();
        }
        return blockPrimaryStorageHostRefVOList;
    }

    @Override
    public List<String> getEdgeNames() {
        //return Arrays.asList(HostVO.class.getSimpleName(), BlockPrimaryStorageVO.class.getSimpleName());
        return Arrays.asList(HostVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction cascadeAction) {
        return null;
    }
}
