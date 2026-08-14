package org.zstack.core.workflow;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Od;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.statemachine.StateMachine;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.zstack.core.Platform.inerr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class WorkFlowChain {
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
    protected WorkFlowContext context;
    protected List<WorkFlow> flows = new ArrayList<WorkFlow>();
    protected String uuid;
    protected WorkFlowChainVO chainvo;

    public WorkFlowChain(String name) {
        this.name = name;
    }
    
    public WorkFlowChain() {
        this.name = "zstack";
    }

    public WorkFlowChain add(WorkFlow flow) {
        flows.add(flow);
        return this;
    }

    public WorkFlowChain build() {
        if (this.owner == null) {
            this.owner = "zstack";
        }
        
        if (flows.isEmpty()) {
            throw new IllegalArgumentException("WorkFlowChain cannot be built without adding any WorkFlow in it");
        }

        StringBuilder sb = new StringBuilder(getName());
        sb.append(getOwner());
        for (WorkFlow f : flows) {
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

    public WorkFlowChain setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public WorkFlowChain setName(String name) {
        this.name = name;
        return this;
    }

    protected void initialize() {
        if (getUuid() == null) {
            throw new IllegalArgumentException("WorkFlowChain cannot run before WorkFlowChain.build() is called");
        }

        WorkFlowChainVO cvo = new WorkFlowChainVO();
        cvo.setName(getName());
        cvo.setOwner(owner);
        cvo.setUuid(getUuid());
        cvo.setState(WorkFlowChainState.Processing);
        cvo.setCurrentPosition(0);
        cvo.setTotalWorkFlows(flows.size());
        chainvo = dbf.persistAndRefresh(cvo);
    }

    protected ErrorCode processFlow(WorkFlow flow, WorkFlowVO vo, int position) {
        if (vo == null) {
            vo = new WorkFlowVO();
        }
        vo.setChainUuid(chainvo.getUuid());
        vo.setName(flow.getName());
        vo.setState(WorkFlowState.Processing);
        vo.setContext(context.toBytes());
        vo.setPosition(position);
        vo = dbf.updateAndRefresh(vo);
        try {
            flow.process(context);
            vo.setState(flowState.getNextState(vo.getState(), WorkFlowStateEvent.done));
            vo.setContext(context.toBytes());
            dbf.update(vo);
            logger.debug(String.format("Successfully processed workflow[%s] in chain[%s]", flow.getName(), getName()));
            return null;
        } catch (WorkFlowException e) {
            vo.setReason(e.getErrorCode().toString());
            vo.setState(flowState.getNextState(vo.getState(), WorkFlowStateEvent.failed));
            logger.debug(String.format("workflow[%s] in chain[%s] failed because %s", flow.getName(), getName(), e.getErrorCode()));
            dbf.update(vo);
            return e.getErrorCode();
        } catch (Throwable t) {
            ErrorCode err = inerr(t.getMessage());
            vo.setReason(err.toString());
            vo.setState(flowState.getNextState(vo.getState(), WorkFlowStateEvent.failed));
            logger.debug(String.format("workflow[%s] in chain[%s] failed because of an unhandle exception", flow.getName(), getName()), t);
            dbf.update(vo);
            return err;
        }
    }

    protected void rollbackFlow(WorkFlowVO vo) {
        WorkFlow flow = flows.get(vo.getPosition());
        WorkFlowContext ctx = WorkFlowContext.fromBytes(vo.getContext());
        try {
            flow.rollback(ctx);
            logger.debug(String.format("Successfully rolled back workflow[%s] in chain[%s]", flow.getName(), getName()));
        } catch (Throwable t) {
            logger.warn(String.format("Unhandled exception happend while rolling back workflow[%s] in chain[%s]", flow.getName(), getName()), t);
        }
        vo.setState(flowState.getNextState(vo.getState(), WorkFlowStateEvent.rollbackDone));
        dbf.update(vo);
    }

    protected void rollback() {
        SimpleQuery<WorkFlowVO> query = dbf.createQuery(WorkFlowVO.class);
        query.add(WorkFlowVO_.chainUuid, Op.EQ, chainvo.getUuid());
        query.orderBy(WorkFlowVO_.position, Od.DESC);
        List<WorkFlowVO> vos = query.list();
        for (WorkFlowVO vo : vos) {
            if (vo.getState() == WorkFlowState.RollbackDone) {
                /* when this is called from carryOn(), some flows may have been rolled back, skip them */
                continue;
            }
            rollbackFlow(vo);
        }
        chainvo.setState(chainStates.getNextState(chainvo.getState(), WorkFlowChainStateEvent.rollbackDone));
        chainvo = dbf.updateAndRefresh(chainvo);
        logger.debug(String.format("Rolled back all flows in workflow chain[%s]", getName()));
    }

    public WorkFlowContext run() throws WorkFlowException {
        return run(null);
    }
    
    public WorkFlowContext run(WorkFlowContext ctx) throws WorkFlowException {
        if (ctx == null) {
            ctx = new WorkFlowContext();
        }
        
        initialize();
        this.context = ctx;
        ErrorCode err = null;
        
        for (int i = 0; i < flows.size(); i++) {
            WorkFlow flow = flows.get(i);
            err = processFlow(flow, null, i);
            if (err != null) {
                chainvo.setReason(err.toString());
                chainvo.setState(chainStates.getNextState(chainvo.getState(), WorkFlowChainStateEvent.failed));
                chainvo.setCurrentPosition(i);
                chainvo = dbf.updateAndRefresh(chainvo);
                break;
            } else {
                chainvo.setCurrentPosition(i);
                chainvo = dbf.updateAndRefresh(chainvo);
            }
        }

        if (err != null) {
            logger.debug(String.format("Starting to roll back all flows in chain[%s]", getName()));
            rollback();
            throw new WorkFlowException(err);
        } else {
            chainvo.setState(WorkFlowChainState.ProcessDone);
            chainvo = dbf.updateAndRefresh(chainvo);
        }

        return this.context;
    }
    
    protected WorkFlowContext run(WorkFlowContext ctx, WorkFlowVO vo) throws WorkFlowException {
        this.context = ctx;
        ErrorCode err = null;
        
        int startPosition = vo.getState() == WorkFlowState.Done ? vo.getPosition() + 1 : vo.getPosition();
        logger.debug(String.format("Restart flows in work flow chain[uuid:%s], start position is %s", chainvo.getUuid(), startPosition));
        for (int i = startPosition; i < flows.size(); i++) {
            WorkFlow flow = flows.get(i);
            if (vo.getPosition() == i) {
                err = processFlow(flow, vo, i);
            } else {
                err = processFlow(flow, null, i);
            }
            
            if (err != null) {
                chainvo.setReason(err.toString());
                chainvo.setState(chainStates.getNextState(chainvo.getState(), WorkFlowChainStateEvent.failed));
                chainvo.setCurrentPosition(i);
                chainvo = dbf.updateAndRefresh(chainvo);
                break;
            } else {
                chainvo.setCurrentPosition(i);
                chainvo = dbf.updateAndRefresh(chainvo);
            }
        }

        if (err != null) {
            logger.debug(String.format("Starting to roll back all flows in chain[%s]", getName()));
            rollback();
            throw new WorkFlowException(err);
        } else {
            chainvo.setState(WorkFlowChainState.ProcessDone);
            chainvo = dbf.updateAndRefresh(chainvo);
        }

        return this.context;
    }

    protected ContinueStrategy getContinueStrategy() {
        if (chainvo.getState() == WorkFlowChainState.ProcessDone || chainvo.getState() == WorkFlowChainState.RollbackDone) {
            return ContinueStrategy.Nothing;
        } else if (chainvo.getState() == WorkFlowChainState.ProcessFailed) {
            return ContinueStrategy.Rollback;
        } else if (chainvo.getState() == WorkFlowChainState.Processing) {
            SimpleQuery<WorkFlowVO> query = dbf.createQuery(WorkFlowVO.class);
            query.add(WorkFlowVO_.chainUuid, Op.EQ, chainvo.getUuid());
            query.orderBy(WorkFlowVO_.position, Od.DESC);
            List<WorkFlowVO> vos = query.list();
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

    public WorkFlowContext carryOn(String chainUuid) throws WorkFlowException {
        SimpleQuery<WorkFlowChainVO> query = dbf.createQuery(WorkFlowChainVO.class);
        query.add(WorkFlowChainVO_.uuid, Op.EQ, chainUuid);
        chainvo = query.find();
        if (chainvo == null) {
            throw new IllegalArgumentException(String.format("Cannot find workflow chain[uuid:%s]", chainUuid));
        }
        
        this.name = chainvo.getName();
        this.owner = chainvo.getOwner();
        this.uuid = chainvo.getUuid();

        ContinueStrategy nextStep = getContinueStrategy();
        if (nextStep == ContinueStrategy.Nothing) {
            return carryOnNothing();
        } else if (nextStep == ContinueStrategy.Restart) {
            return carryOnRestart();
        } else if (nextStep == ContinueStrategy.Rollback) {
            return carryOnRollback();
        }

        return null;
    }

    protected WorkFlowContext carryOnRollback() {
        logger.debug(String.format("Restart to roll back flows in work flow chain[uuid:%s]", chainvo.getUuid()));
        rollback();
        return null;
    }

    protected WorkFlowContext carryOnRestart() throws WorkFlowException {
        SimpleQuery<WorkFlowVO> query = dbf.createQuery(WorkFlowVO.class);
        query.add(WorkFlowVO_.chainUuid, Op.EQ, chainvo.getUuid());
        query.orderBy(WorkFlowVO_.position, Od.DESC);
        List<WorkFlowVO>  vos = query.list();
        WorkFlowVO last = vos.get(0);
        assert last.getState() == WorkFlowState.Done || last.getState() == WorkFlowState.Processing : String.format("How can work flow[%s] in %s state when restart workflow chain[uuid:%s] !!?", last.getName(), last.getState(), chainvo.getUuid());
        return run(WorkFlowContext.fromBytes(last.getContext()), last);
    }

    protected WorkFlowContext carryOnNothing() {
        logger.debug(String.format("Noting to carry on for work flow chain[uuid:%s]", chainvo.getUuid()));
        if (chainvo.getState() == WorkFlowChainState.ProcessDone) {
            SimpleQuery<WorkFlowVO> query = dbf.createQuery(WorkFlowVO.class);
            query.add(WorkFlowVO_.chainUuid, Op.EQ, chainvo.getUuid());
            query.add(WorkFlowVO_.position, Op.EQ, chainvo.getTotalWorkFlows() - 1);
            WorkFlowVO vo = query.find();
            return WorkFlowContext.fromBytes(vo.getContext());
        } else if (chainvo.getState() == WorkFlowChainState.RollbackDone) {
            return null;
        } else {
            assert false : "Cannot be here";
            return null;
            /*
            SimpleQuery<WorkFlowVO> query = dbf.createQuery(WorkFlowVO.class);
            query.add(WorkFlowVO_.chainUuid, Op.EQ, chainvo.getUuid());
            query.add(WorkFlowVO_.position, Op.EQ, chainvo.getTotalWorkFlows() - 1);
            WorkFlowVO vo = query.find();
            if (vo.getState() == WorkFlowState.Done) {
                chainvo.setState(WorkFlowChainState.ProcessDone);
                chainvo.setCurrentPosition(chainvo.getTotalWorkFlows()-1);
                chainvo = dbf.update(chainvo);
                return WorkFlowContext.fromBytes(vo.getContext());
            } else if (vo.getState() == WorkFlowState.RollbackDone) {
                query = dbf.createQuery(WorkFlowVO.class);
                query.select(WorkFlowVO_.reason);
                query.add(WorkFlowVO_.chainUuid, Op.EQ, chainvo.getUuid());
                query.add(WorkFlowVO_.reason, Op.NOT_NULL);
                String reason = query.findValue();
                chainvo.setReason(reason);
                chainvo.setState(WorkFlowChainState.RollbackDone);
                chainvo.setCurrentPosition(chainvo.getTotalWorkFlows()-1);
                chainvo = dbf.update(chainvo);
                return null;
            } else {
                throw new CloudRuntimeException(String.mediaType("Program error: the latest work flow[%s] is in %s state, but work flow chain[uuid:%s] is in %s state", vo.getName(), vo.getState(), chainvo.getUuid(), chainvo.getState()));
            }
            */
        }
    }
}
