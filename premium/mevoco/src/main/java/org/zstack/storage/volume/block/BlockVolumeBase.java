package org.zstack.storage.volume.block;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.block.APIUpdateBlockVolumeEvent;
import org.zstack.header.volume.block.APIUpdateBlockVolumeMsg;
import org.zstack.header.volume.block.BlockVolumeInventory;
import org.zstack.header.volume.block.BlockVolumeVO;
import org.zstack.storage.volume.VolumeBase;

import java.util.Map;

public class BlockVolumeBase extends VolumeBase {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;

    public BlockVolumeBase(VolumeVO vo) {
        super(vo);
    }

    protected BlockVolumeVO getSelf() {
        return (BlockVolumeVO) self;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIUpdateBlockVolumeMsg) {
            handle((APIUpdateBlockVolumeMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    protected void handle(APIUpdateBlockVolumeMsg msg) {
        final APIUpdateBlockVolumeEvent evt = new APIUpdateBlockVolumeEvent(msg.getId());
        doUpdateBlockVolumeInQueue(msg, new ReturnValueCompletion<BlockVolumeInventory>(msg) {
            @Override
            public void success(BlockVolumeInventory inventory) {
                evt.setInventory(inventory);
                bus.publish(evt);
            }
            
            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void doUpdateBlockVolumeInQueue(APIUpdateBlockVolumeMsg msg, ReturnValueCompletion<BlockVolumeInventory> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("do-update-block-volume-%s", msg.getUuid());
            }
            
            @Override
            public void run(SyncTaskChain chain) {
                doUpdateBlockVolume(msg, new ReturnValueCompletion<BlockVolumeInventory>(completion, chain) {
                    @Override
                    public void success(BlockVolumeInventory returnValue) {
                        completion.success(returnValue);
                        chain.next();
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }
            
            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void doUpdateBlockVolume(final BlockVolumeMessage msg, ReturnValueCompletion<BlockVolumeInventory> completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();

        chain.setName(String.format("do-update-block-volume-%s", msg.getBlockVolumeUuid()));

        chain.then(new NoRollbackFlow() {
            String __name__ = "do-update-hook";
            
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                UpdateHook(msg, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }
                    
                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "do-update-block-volume-vo";
            
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                UpdateVO(msg, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                refreshVO();
                completion.success(BlockVolumeInventory.valueOf(getSelf()));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    protected void UpdateVO(BlockVolumeMessage msg, Completion completion) {
        BlockVolumeVO vo = dbf.findByUuid(msg.getBlockVolumeUuid(), BlockVolumeVO.class);
        boolean update = false;
        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            dbf.updateAndRefresh(vo);
        }
        completion.success();
    }

    protected void UpdateHook(BlockVolumeMessage msg, Completion completion) {
        completion.success();
    }
}
