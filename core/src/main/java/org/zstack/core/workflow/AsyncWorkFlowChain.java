package org.zstack.core.workflow;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.statemachine.StateMachine;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.inerr;
import static org.zstack.core.Platform.operr;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AsyncWorkFlowChain {
    protected static final CLogger logger = Utils.getLogger(WorkFlowChain.class);
    protected static final StateMachine<WorkFlowChainState, WorkFlowChainStateEvent> chainStates;
    protected static final StateMachine<WorkFlowState, WorkFlowStateEvent> flowState;

    static {
        chainStates = Platform.<WorkFlowChainState, WorkFlowChainStateEvent> createStateMachine();
        chainStates.addTranscation(WorkFlowChainState.Processing, WorkFlowChainStateEvent.done, WorkFlowChainState.ProcessDone);
        chainStates.addTranscation(WorkFlowChainState.Processing, WorkFlowChainStateEvent.failed, WorkFlowChainState.ProcessFailed);
        chainStates.addTranscation(WorkFlowChainState.ProcessFailed, WorkFlowChainStateEvent.rollbackDone, WorkFlowChainState.RollbackDone);
        chainStates.addTranscation(WorkFlowChainState.ProcessDone, WorkFlowChainStateEvent.rollbackDone, WorkFlowChainState.RollbackDone);

        flowState = Platform.<WorkFlowState, WorkFlowStateEvent> createStateMachine();
        flowState.addTranscation(WorkFlowState.Processing, WorkFlowStateEvent.done, WorkFlowState.Done);
        flowState.addTranscation(WorkFlowState.Processing, WorkFlowStateEvent.failed, WorkFlowState.Failed);
        flowState.addTranscation(WorkFlowState.Failed, WorkFlowStateEvent.rollbackDone, WorkFlowState.RollbackDone);
        flowState.addTranscation(WorkFlowState.Done, WorkFlowStateEvent.rollbackDone, WorkFlowState.RollbackDone);
    }

    protected enum ContinueStrategy {
        Restart, Nothing, Rollback,
    }

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;

    protected String name;
    protected String owner;
    protected List<AsyncWorkFlow> flows = new ArrayList<>();
    protected String uuid;
    protected WorkFlowChainVO chainvo;
    protected WorkFlowCallback callback;
    protected int currentPosition = 0;
    private boolean isInitialized = false;
    private WorkFlowContext context;
    
    public AsyncWorkFlowChain() {
        this.name = "zstack";
    }
    
    public AsyncWorkFlowChain(String name) {
        this.name = name;
    }

    public AsyncWorkFlowChain add(AsyncWorkFlow flow) {
        flows.add(flow);
        return this;
    }

    public AsyncWorkFlowChain build() {
        if (this.owner == null) {
            this.owner = "zstack";
        }
        
        if (flows.isEmpty()) {
            throw new IllegalArgumentException("AsyncWorkFlowChain cannot be built without adding any WorkFlow in it");
        }

        StringBuilder sb = new StringBuilder(getName());
        sb.append(getOwner());
        for (AsyncWorkFlow f : flows) {
            String name = f.getName();
            name = name == null ? f.getClass().getCanonicalName() : name;
            sb.append(name);
        }

        uuid = UUID.nameUUIDFromBytes(sb.toString().getBytes()).toString().replace("-", "");
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public AsyncWorkFlowChain setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public AsyncWorkFlowChain setName(String name) {
        this.name = name;
        return this;
    }

    protected void initialize() {
        if (getUuid() == null) {
            throw new IllegalArgumentException("WorkFlowChain cannot run before WorkFlowChain.build() is called");
        }

        dbf.removeByPrimaryKey(getUuid(), WorkFlowChainVO.class);
        WorkFlowChainVO cvo = new WorkFlowChainVO();
        cvo.setName(getName());
        cvo.setOwner(owner);
        cvo.setUuid(getUuid());
        cvo.setState(WorkFlowChainState.Processing);
        cvo.setCurrentPosition(0);
        cvo.setTotalWorkFlows(flows.size());
        chainvo = dbf.persistAndRefresh(cvo);
    }
    
    protected void processFlow(AsyncWorkFlow flow, WorkFlowContext ctx, WorkFlowVO vo, int position) {
        if (vo == null) {
            vo = new WorkFlowVO();
        }
        vo.setChainUuid(chainvo.getUuid());
        vo.setName(flow.getName());
        vo.setState(WorkFlowState.Processing);
        vo.setContext(ctx.toBytes());
        vo.setPosition(position);
        vo = dbf.updateAndRefresh(vo);
        try {
            flow.process(ctx, this);
        } catch (WorkFlowException e) {
            try {
                fail(vo, e.getErrorCode());
            } catch (Throwable t) {
                logger.warn(String.format("Something seriously wrong happened when roll back"), t);
            }
        } catch (Throwable t) {
            logger.warn(String.format("workflow[%s] in chain[%s] failed because of an unhandle exception", flow.getName(), getName()), t);
            ErrorCode err = inerr(t.getMessage());
            try {
                fail(vo, err);
            } catch (Throwable t1) {
                logger.warn(String.format("Something seriously wrong happened when roll back"), t1);
            }
        }
    }
    
    private WorkFlowVO getFlowVOByPosition(int position) {
        WorkFlowVO vo = Q.New(WorkFlowVO.class)
                .eq(WorkFlowVO_.position, position)
                .eq(WorkFlowVO_.chainUuid, chainvo.getUuid())
                .find();
        assert vo != null : "Where is WorkFlowVO for position: " + position;
        return vo;
    }
    
    private void tellCallbackSuccess(WorkFlowContext ctx) {
        try {
            callback.succeed(ctx);
        } catch (Throwable t) {
            logger.warn(String.format("Unhandled exception in WorkFlowCallback[%s]", callback.getClass().getCanonicalName()), t);
        } 
    }
    
    private void tellCallbackFailure(WorkFlowContext ctx, ErrorCode err) {
        try {
            callback.fail(ctx, err);
        } catch (Throwable t) {
            logger.warn(String.format("Unhandled exception in WorkFlowCallback[%s]", callback.getClass().getCanonicalName()), t);
        }
    }
    
    public void runNext(WorkFlowContext ctx) {
        if (!isInitialized) {
            throw new CloudRuntimeException(String.format("runNext() can only be called from AysncWorkFlow"));
        }
        
        WorkFlowVO vo = getFlowVOByPosition(currentPosition);
        vo.setState(flowState.getNextState(vo.getState(), WorkFlowStateEvent.done));
        vo.setContext(ctx.toBytes());
        dbf.update(vo);
        logger.debug(String.format("Successfully processed workflow[%s] in chain[%s]", vo.getName(), getName()));
        currentPosition++;
        if (currentPosition < flows.size()) {
            chainvo.setCurrentPosition(currentPosition);
            chainvo = dbf.updateAndRefresh(chainvo);
            AsyncWorkFlow flow = flows.get(currentPosition);
            processFlow(flow, ctx, null, currentPosition);
        } else {
            chainvo.setState(WorkFlowChainState.ProcessDone);
            chainvo = dbf.updateAndRefresh(chainvo);
            tellCallbackSuccess(ctx);
        }
    }
    
    protected void rollbackFlow(WorkFlowVO vo) {
        AsyncWorkFlow flow = flows.get(vo.getPosition());
        WorkFlowContext ctx = WorkFlowContext.fromBytes(vo.getContext());
        try {
            flow.rollback(ctx);
            logger.debug(String.format("Successfully rolled back AsyncWorkFlow[%s] in chain[%s]", flow.getName(), getName()));
        } catch (Throwable t) {
            logger.warn(String.format("Unhandled exception happend while rolling back AsyncWorkFlow[%s] in chain[%s]", flow.getName(), getName()), t);
        }
        vo.setState(flowState.getNextState(vo.getState(), WorkFlowStateEvent.rollbackDone));
        dbf.update(vo);
    }
    
    public void rollback() {
        List<WorkFlowVO> vos = Q.New(WorkFlowVO.class)
                .eq(WorkFlowVO_.chainUuid, chainvo.getUuid())
                .orderByDesc(WorkFlowVO_.position)
                .list();
        logger.debug(String.format("starting to rollback AsyncWorkFlowChain[name: %s, owner: %s]", name, owner));
        for (WorkFlowVO vo : vos) {
            if (vo.getState() == WorkFlowState.RollbackDone) {
                /* when this is called from carryOn(), some flows may have been rolled back, skip them */
                continue;
            }
            rollbackFlow(vo);
        }
        chainvo.setState(chainStates.getNextState(chainvo.getState(), WorkFlowChainStateEvent.rollbackDone));
        chainvo = dbf.updateAndRefresh(chainvo);
        logger.debug(String.format("Rolled back all flows in AsyncWorkFlow chain[%s]", getName()));
    }
    
    private void fail(WorkFlowVO vo, ErrorCode err) {
        vo.setReason(err.toString());
        vo.setState(flowState.getNextState(vo.getState(), WorkFlowStateEvent.failed));
        logger.debug(String.format("workflow[%s] in chain[%s] failed because %s", vo.getName(), getName(), err));
        dbf.update(vo);
        
        chainvo.setReason(err.toString());
        chainvo.setState(chainStates.getNextState(chainvo.getState(), WorkFlowChainStateEvent.failed));
        chainvo.setCurrentPosition(vo.getPosition());
        chainvo = dbf.updateAndRefresh(chainvo);
        rollback();
        WorkFlowContext ctx = WorkFlowContext.fromBytes(vo.getContext());
        tellCallbackFailure(ctx, err);
    }
    
    public void fail(AsyncWorkFlow flow, ErrorCode err) {
        int position = flows.indexOf(flow);
        WorkFlowVO vo = getFlowVOByPosition(position);
        fail(vo, err);
    }
    
    public void run(WorkFlowCallback callback) {
        run(null, callback);
    }
    
    public void run(WorkFlowContext ctx, WorkFlowCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback can not be null");
        }
        
        if (ctx == null) {
            ctx = new WorkFlowContext();
        }
        this.callback = callback;
        initialize();
        isInitialized = true;
        logger.debug(String.format("starting to run AsyncWorkFlowChain[name: %s, owner: %s]", name, owner));
        AsyncWorkFlow flow = flows.get(currentPosition);
        processFlow(flow, ctx, null, currentPosition);
    }
    
    protected ContinueStrategy getContinueStrategy() {
        if (chainvo.getState() == WorkFlowChainState.ProcessDone || chainvo.getState() == WorkFlowChainState.RollbackDone) {
            return ContinueStrategy.Nothing;
        } else if (chainvo.getState() == WorkFlowChainState.ProcessFailed) {
            return ContinueStrategy.Rollback;
        } else if (chainvo.getState() == WorkFlowChainState.Processing) {
            List<WorkFlowVO> vos = Q.New(WorkFlowVO.class)
                    .eq(WorkFlowVO_.chainUuid, chainvo.getUuid())
                    .orderByDesc(WorkFlowVO_.position)
                    .list();
            if (vos.isEmpty()) {
                return ContinueStrategy.Restart;
            }

            WorkFlowVO last = vos.get(0);
            if (last.getState() == WorkFlowState.Processing) {
                return ContinueStrategy.Restart;
            } else if (last.getState() == WorkFlowState.Done) {
                return last.getPosition() == chainvo.getTotalWorkFlows() - 1 ? ContinueStrategy.Nothing : ContinueStrategy.Restart;
            } else if (last.getState() == WorkFlowState.RollbackDone) {
                WorkFlowVO first = vos.get(vos.size() - 1);
                return first.getState() == WorkFlowState.RollbackDone ? ContinueStrategy.Nothing : ContinueStrategy.Rollback;
            } else if (last.getState() == WorkFlowState.Failed) {
                return ContinueStrategy.Rollback;
            }
        }

        throw new CloudRuntimeException(String.format("Program error: cannot find ContinueStrategy for work flow chain[uuid:%s]", chainvo.getUuid()));
    }
    
    public void carryOn(String chainUuid, WorkFlowCallback callback) throws WorkFlowException {
        if (callback == null) {
            throw new IllegalArgumentException("callback can not be null");
        }
        
        chainvo = Q.New(WorkFlowChainVO.class)
                .eq(WorkFlowChainVO_.uuid, chainUuid)
                .find();
        if (chainvo == null) {
            throw new IllegalArgumentException(String.format("Cannot find workflow chain[uuid:%s]", chainUuid));
        }
        
        this.callback = callback;
        this.name = chainvo.getName();
        this.owner = chainvo.getOwner();
        this.uuid = chainvo.getUuid();
        this.isInitialized = true;

        ContinueStrategy nextStep = getContinueStrategy();
        if (nextStep == ContinueStrategy.Nothing) {
            carryOnNothing();
        } else if (nextStep == ContinueStrategy.Restart) {
            carryOnRestart();
        } else if (nextStep == ContinueStrategy.Rollback) {
            carryOnRollback();
        }
    }
    
    protected void carryOnRollback() {
        logger.debug(String.format("Restart to roll back flows in work AsyncWorkFlowChain[uuid:%s]", chainvo.getUuid()));
        WorkFlowVO failedFlow = Q.New(WorkFlowVO.class)
                .eq(WorkFlowVO_.chainUuid, chainvo.getUuid())
                .notNull(WorkFlowVO_.reason)
                .find();
        rollback();
        WorkFlowContext ctx = WorkFlowContext.fromBytes(failedFlow.getContext());
        ErrorCode err = operr(failedFlow.getReason());
        tellCallbackFailure(ctx, err);
    }
    
    protected void carryOnRestart() throws WorkFlowException {
        List<WorkFlowVO> vos = Q.New(WorkFlowVO.class)
                .eq(WorkFlowVO_.chainUuid, chainvo.getUuid())
                .orderByDesc(WorkFlowVO_.position)
                .list();
        WorkFlowVO last = vos.get(0);
        assert last.getState() == WorkFlowState.Done || last.getState() == WorkFlowState.Processing : String.format("How can work flow[%s] in %s state when restart workflow chain[uuid:%s] !!?", last.getName(), last.getState(), chainvo.getUuid());
        int startPosition;
        WorkFlowVO startVO;
        if (last.getState() == WorkFlowState.Done) {
            startPosition = last.getPosition() + 1;
            startVO = null;
        } else {
            startPosition = last.getPosition();
            startVO = last;
        }
        logger.debug(String.format("Restart flows in work flow chain[uuid:%s], start position is %s", chainvo.getUuid(), startPosition));
        WorkFlowContext ctx = WorkFlowContext.fromBytes(last.getContext());
        AsyncWorkFlow flow = flows.get(startPosition);
        this.currentPosition = startPosition;
        processFlow(flow, ctx, startVO, startPosition);
    }
    
    protected void carryOnNothing() {
        logger.debug(String.format("Noting to carry on for work flow chain[uuid:%s]", chainvo.getUuid()));
    }

    public WorkFlowContext getContext() {
        return context;
    }

    public void setContext(WorkFlowContext context) {
        this.context = context;
    }
}
