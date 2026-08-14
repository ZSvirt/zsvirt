package org.zstack.core.checkpoint;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.statemachine.StateMachine;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.serializable.SerializableHelper;

import java.io.IOException;
import java.util.Arrays;

public aspect CheckPointAspect {
    @Autowired
    DatabaseFacade dbf;
    private static final CLogger logger = Utils.getLogger(CheckPointAspect.class);
    private static final StateMachine<CheckPointState, CheckPointStateEvent> states;

    static {
        states = Platform.<CheckPointState, CheckPointStateEvent> createStateMachine();
        states.addTranscation(CheckPointState.Creating, CheckPointStateEvent.ExeSuccessful, CheckPointState.ExecutedSuccessful);
        states.addTranscation(CheckPointState.Creating, CheckPointStateEvent.ExeFailed, CheckPointState.ExecutedFailed);
        states.addTranscation(CheckPointState.ExecutedFailed, CheckPointStateEvent.ExeFailed, CheckPointState.ExecutedFailed);
        states.addTranscation(CheckPointState.ExecutedFailed, CheckPointStateEvent.ExeSuccessful, CheckPointState.ExecutedSuccessful);
        states.addTranscation(CheckPointState.ExecutedFailed, CheckPointStateEvent.CleanFailed, CheckPointState.CleanUpFailed);
        states.addTranscation(CheckPointState.ExecutedFailed, CheckPointStateEvent.CleanSuccessful, CheckPointState.CleanUpSuccessful);
        states.addTranscation(CheckPointState.ExecutedSuccessful, CheckPointStateEvent.ExeSuccessful, CheckPointState.ExecutedSuccessful);
        states.addTranscation(CheckPointState.ExecutedSuccessful, CheckPointStateEvent.ExeFailed, CheckPointState.ExecutedFailed);
        states.addTranscation(CheckPointState.ExecutedSuccessful, CheckPointStateEvent.CleanFailed, CheckPointState.CleanUpFailed);
        states.addTranscation(CheckPointState.ExecutedSuccessful, CheckPointStateEvent.CleanSuccessful, CheckPointState.CleanUpSuccessful);
        states.addTranscation(CheckPointState.CleanUpFailed, CheckPointStateEvent.ExeSuccessful, CheckPointState.ExecutedSuccessful);
        states.addTranscation(CheckPointState.CleanUpFailed, CheckPointStateEvent.ExeFailed, CheckPointState.ExecutedFailed);
        states.addTranscation(CheckPointState.CleanUpFailed, CheckPointStateEvent.CleanFailed, CheckPointState.CleanUpFailed);
        states.addTranscation(CheckPointState.CleanUpFailed, CheckPointStateEvent.CleanSuccessful, CheckPointState.CleanUpSuccessful);
        states.addTranscation(CheckPointState.CleanUpSuccessful, CheckPointStateEvent.ExeSuccessful, CheckPointState.ExecutedSuccessful);
        states.addTranscation(CheckPointState.CleanUpSuccessful, CheckPointStateEvent.ExeFailed, CheckPointState.ExecutedFailed);
        states.addTranscation(CheckPointState.CleanUpSuccessful, CheckPointStateEvent.CleanFailed, CheckPointState.CleanUpFailed);
        states.addTranscation(CheckPointState.CleanUpSuccessful, CheckPointStateEvent.CleanSuccessful, CheckPointState.CleanUpSuccessful);
    }

    pointcut CheckPointProxyExecute(CheckPointProxy cpp) : target(cpp) && execution(void CheckPointProxy.execute());

    pointcut CheckPointEntryExecute(CheckPoint cp) : within(CheckPoint+) && this(cp) && call(@ChkPoint * *.*(..));

    pointcut CheckPointProxyCleanUp(CheckPointProxy cpp) : target(cpp) && execution(void CheckPointProxy.cleanUp());

    pointcut CheckPointEntryCleanUp(CheckPoint cp, CheckPointContext ctx) : within(CheckPoint+) && this(cp) && args(ctx, ..) && call(@ChkCleanUp * *.*(CheckPointContext, ..));

    private CheckPointProxy CheckPoint.proxyObject = null;

    private CheckPointVO findCheckPointVO(String uuid) {
        SimpleQuery<CheckPointVO> query = dbf.createQuery(CheckPointVO.class);
        query.add(CheckPointVO_.uuid, Op.EQ, uuid);
        CheckPointVO vo = query.find();
        return vo;
    }

    before(CheckPointProxy cpp) : CheckPointProxyCleanUp(cpp) {
        try {
            CheckPointVO vo = null;
            CheckPoint cp = cpp.getCheckPoint();

            cp.proxyObject = cpp;
            vo = findCheckPointVO(cpp.getCheckPointUuid());
            if (vo == null) {
                logger.warn("Cannot find CheckPoint for uuid=" + cpp.getCheckPointUuid() + ", cleanup will not execute");
                return;
            }

            if (cpp.isReloadInput()) {
                cp = reloadChkPointInput(cp, vo);
            }

            cpp.setCheckPointVO(vo);
        } catch (Exception e) {
            throw new CloudCheckPointException("[CheckPoint internal error]", e);
        }
    }

    private CheckPoint reloadChkPointInput(CheckPoint cp, CheckPointVO vo) throws ClassNotFoundException, IOException {
        byte[] bctx = vo.getContext();
        ChkPointInputContext ctx;
        ctx = SerializableHelper.readObject(bctx);
        return ctx.load(cp);
    }

    before(CheckPointProxy cpp) : CheckPointProxyExecute(cpp) {
        try {
            CheckPointVO vo = null;
            CheckPoint cp = cpp.getCheckPoint();

            cp.proxyObject = cpp;
            if (cpp.getCheckPointUuid() != null) {
                vo = findCheckPointVO(cpp.getCheckPointUuid());
                if (vo == null) {
                    logger.warn("Cannot find CheckPoint for uuid=" + cpp.getCheckPointUuid() + ", execute as a new check point");
                }
            }

            if (vo == null) {
                vo = new CheckPointVO(cp.getClass().getCanonicalName());
                byte[] bctx;
                ChkPointInputContext ctx = new ChkPointInputContext(cp);
                bctx = SerializableHelper.writeObject(ctx);
                vo.setContext(bctx);
                dbf.persist(vo);
            } else {
                if (cpp.isReloadInput()) {
                    cp = reloadChkPointInput(cp, vo);
                }
            }

            cpp.setCheckPointVO(vo);
        } catch (Exception e) {
            throw new CloudCheckPointException("[CheckPoint internal Error]", e);
        }
    }

    private void checkPointExecuteResult(CheckPointProxy cpp, boolean success) {
        CheckPointVO vo = cpp.getCheckPointVO();
        assert vo != null : "Where is my CheckPointVO for " + cpp.getCheckPoint().getClass().getName();
        CheckPointState next;
        if (success) {
            next = states.getNextState(vo.getState(), CheckPointStateEvent.ExeSuccessful);
        } else {
            next = states.getNextState(vo.getState(), CheckPointStateEvent.ExeFailed);
        }
        vo.setState(next);
        dbf.update(vo);
    }

    after(CheckPointProxy cpp) returning: CheckPointProxyExecute(cpp) {
        try {
            checkPointExecuteResult(cpp, true);
        } catch (Exception e) {
            throw new CloudCheckPointException("[CheckPoint internal Error]", e);
        }
    }

    after(CheckPointProxy cpp) throwing(Throwable t): CheckPointProxyExecute(cpp) {
        if (t instanceof CloudCheckPointException) {
            /* Internal error happened, let it out */
        } else {
            try {
                checkPointExecuteResult(cpp, false);
            } catch (Exception e) {
                /*
                 * We can not throw out CloudCheckPointException here, otherwise
                 * the original Throwable will miss, anyway, we record error here
                 * for debugging
                 */
                logger.warn("[CheckPoint internal Error]", e);
            }
        }
    }

    private void checkPointCleanupResult(CheckPointProxy cpp, boolean success) {
        if (cpp.getCheckPointVO() == null) {
            return;
        }

        CheckPointVO vo = cpp.getCheckPointVO();
        CheckPointState next;
        if (success) {
            next = states.getNextState(vo.getState(), CheckPointStateEvent.CleanSuccessful);
        } else {
            next = states.getNextState(vo.getState(), CheckPointStateEvent.CleanFailed);
        }
        vo.setState(next);
        dbf.update(vo);
    }

    after(CheckPointProxy cpp) returning: CheckPointProxyCleanUp(cpp) {
        try {
            checkPointCleanupResult(cpp, true);
        } catch (Exception e) {
            throw new CloudCheckPointException("[CheckPoint internal Error]", e);
        }
    }

    after(CheckPointProxy cpp) throwing(Throwable t): CheckPointProxyCleanUp(cpp) {
        if (t instanceof CloudCheckPointException) {
            /* Internal error happened, let it out */
        } else {
            try {
                checkPointCleanupResult(cpp, false);
            } catch (Exception e) {
                /*
                 * We can not throw out CloudCheckPointException here, otherwise
                 * the original Throwable will miss, anyway, we record error here
                 * for debugging
                 */
                logger.warn("[CheckPoint internal Error]", e);
            }
        }
    }

    private Object getLastReturn(CheckPointEntryVO vo) throws ClassNotFoundException, IOException {
        byte[] bctxt = vo.getContext();
        CheckPointContext ctx;
        ctx = SerializableHelper.readObject(bctxt);
        return ctx.getOutput();
    }

    private byte[] makeEntryContext(Object[] args, Object ret) throws IOException {
        CheckPointContext ctx = new CheckPointContext(args, ret);
        byte[] ctxbytes = SerializableHelper.writeObject(ctx);
        return ctxbytes;
    }

    private void checkPointEntryFail(CheckPointEntryVO vo, Object[] args, String reason) throws IOException {
        CheckPointState next = states.getNextState(vo.getState(), CheckPointStateEvent.ExeFailed);
        vo.setState(next);
        vo.setReason(reason);
        byte[] bctx = makeEntryContext(args, null);
        vo.setContext(bctx);
        dbf.update(vo);
    }

    private void checkPointEntrySuccess(CheckPointEntryVO vo, Object[] args, Object ret, boolean isSaveCtx) throws IOException {
        CheckPointState next = states.getNextState(vo.getState(), CheckPointStateEvent.ExeSuccessful);
        vo.setState(next);
        vo.setReason(null);
        if (isSaveCtx) {
            byte[] bctx = makeEntryContext(args, ret);
            vo.setContext(bctx);
        }
        dbf.update(vo);
    }

    private boolean isInBypassEntries(CheckPointProxy cpp, String entryName) {
        String[] entries = cpp.getBypassCheckPointEntires();
        if (entries == null) {
            return false;
        }

        return Arrays.binarySearch(entries, entryName) >= 0;
    }

    private String getEntryName(JoinPoint jp) {
        MethodSignature mtd = (MethodSignature) jp.getStaticPart().getSignature();
        ChkPoint at = mtd.getMethod().getAnnotation(ChkPoint.class);
        String name = at.name();
        if ("".equals(name)) {
            name = jp.getSignature().toShortString();
        }

        return name;
    }

    private String getCleanUpCheckPointEntryName(JoinPoint jp) {
        MethodSignature mtd = (MethodSignature) jp.getStaticPart().getSignature();
        ChkCleanUp at = mtd.getMethod().getAnnotation(ChkCleanUp.class);
        return at.checkPointName();
    }

    private CheckPointEntryVO findEntry(CheckPointProxy cpp, String entryName) {
        CheckPointEntryVO entry;
        SimpleQuery<CheckPointEntryVO> query = dbf.createQuery(CheckPointEntryVO.class);
        query.add(CheckPointEntryVO_.checkPointId, Op.EQ, cpp.getCheckPointVO().getId());
        query.add(CheckPointEntryVO_.name, Op.EQ, entryName);
        entry = query.find();
        return entry;
    }

    private String getCleanUpEntryName(JoinPoint jp) {
        MethodSignature mtd = (MethodSignature) jp.getStaticPart().getSignature();
        ChkCleanUp at = mtd.getMethod().getAnnotation(ChkCleanUp.class);
        String name = at.name();
        if ("".equals(name)) {
            name = jp.getSignature().toShortString();
        }

        return name;
    }

    private void cleanUpFail(JoinPoint jp, CheckPoint cp, String failReason) {
        CheckPointEntryVO entry;
        CheckPointProxy cpp = cp.proxyObject;
        assert cpp != null : "Where is my CheckPointProxy???";

        if (isInBypassEntries(cpp, getCleanUpEntryName(jp)) || cpp.getCheckPointVO() == null) {
            return;
        }

        String chkName = getCleanUpCheckPointEntryName(jp);
        entry = findEntry(cpp, chkName);
        if (entry != null) {
            CheckPointState next = states.getNextState(entry.getState(), CheckPointStateEvent.CleanFailed);
            entry.setState(next);
            entry.setReason(failReason);
            dbf.update(entry);
        }
    }

    private void cleanUpSuccess(JoinPoint jp, CheckPoint cp) {
        CheckPointEntryVO entry;
        CheckPointProxy cpp = cp.proxyObject;
        assert cpp != null : "Where is my CheckPointProxy???";

        if (isInBypassEntries(cpp, getCleanUpEntryName(jp)) || cpp.getCheckPointVO() == null) {
            return;
        }

        String chkName = getCleanUpCheckPointEntryName(jp);
        entry = findEntry(cpp, chkName);
        if (entry != null) {
            CheckPointState next = states.getNextState(entry.getState(), CheckPointStateEvent.CleanSuccessful);
            entry.setState(next);
            entry.setReason(null);
            dbf.update(entry);
        }
    }

    void around(CheckPoint cp, CheckPointContext ctx) : CheckPointEntryCleanUp(cp, ctx) {
        try {
            CheckPointEntryVO entry = null;
            String chkName = getCleanUpCheckPointEntryName(thisJoinPoint);
            CheckPointProxy cpp = cp.proxyObject;
            assert cpp != null : "Where is my CheckPointProxy???";

            if (isInBypassEntries(cpp, getCleanUpEntryName(thisJoinPoint)) || cpp.getCheckPointVO() == null) {
                return;
            }

            entry = findEntry(cpp, chkName);
            if (entry == null) {
                logger.warn("Unable to find entry: " + chkName + " for CheckPoint uuid:" + cpp.getCheckPointUuid() + ", cleanup will not execute");
                return;
            }

            CheckPointContext lctx = SerializableHelper.readObject(entry.getContext());
            ctx = lctx;
        } catch (Exception e) {
            throw new CloudCheckPointException("[CheckPoint internal Error]", e);
        }

        proceed(cp, ctx);
    }

    after(CheckPoint cp, CheckPointContext ctx) throwing (Throwable t): CheckPointEntryCleanUp(cp, ctx) {
        if (t instanceof CloudCheckPointException) {
            /* Internal error happened, let it out */
        } else {
            try {
                cleanUpFail(thisJoinPoint, cp, t.getMessage());
            } catch (Exception e) {
                /*
                 * We can not throw out CloudCheckPointException here, otherwise
                 * the original Throwable will miss, anyway, we record error here
                 * for debugging
                 */
                logger.warn("[CheckPoint internal Error]", e);
            }
        }
    }

    after(CheckPoint cp, CheckPointContext ctx) returning: CheckPointEntryCleanUp(cp, ctx) {
        try {
            cleanUpSuccess(thisJoinPoint, cp);
        } catch (Exception e) {
            throw new CloudCheckPointException("[CheckPoint internal Error]", e);
        }
    }

    after(CheckPoint cp) throwing (Throwable t): CheckPointEntryExecute(cp) {
        if (t instanceof CloudCheckPointException) {
            /* Internal error happened, let it out */
        } else {
            try {
                CheckPointEntryVO entry;
                CheckPointProxy cpp = cp.proxyObject;
                assert cpp != null : "Where is my CheckPointProxy???";

                String name = getEntryName(thisJoinPoint);
                entry = findEntry(cpp, name);
                assert entry != null : "Where is my entry for " + name + ", CheckPoint uuid: " + cpp.getCheckPointVO().getUuid() + " ???";
                checkPointEntryFail(entry, thisJoinPoint.getArgs(), t.getMessage());
            } catch (Exception e) {
                /*
                 * We can not throw out CloudCheckPointException here, otherwise
                 * the original Throwable will missanyway, we record error here
                 * for debugging
                 */
                logger.warn("[CheckPoint internal Error]", e);
            }
        }
    }

    Object around(CheckPoint cp): CheckPointEntryExecute(cp) {
        Object ret = null;
        CheckPointEntryVO entry = null;
        try {
            String name = getEntryName(thisJoinPoint);
            CheckPointProxy cpp = cp.proxyObject;
            assert cpp != null : "Where is my CheckPointProxy???";

            if (cpp.getCheckPointUuid() != null) {
                entry = findEntry(cpp, name);
                if (entry == null) {
                    logger.warn("Unable to find entry: " + name + " for CheckPoint uuid:" + cpp.getCheckPointUuid() + ", execute as new CheckPoint entry");
                }
            }

            if (entry != null && !CheckPointState.ExecutedFailed.equals(entry.getState()) && !isInBypassEntries(cpp, name)) {
                try {
                    ret = getLastReturn(entry);
                } catch (Exception e) {
                    String err = "[Unable to reload CheckPointContext] CheckPoint class name" + cp.getClass().getCanonicalName() + ", uuid:"
                            + cpp.getCheckPointUuid() + ", entry: " + name;
                    throw new CloudCheckPointException(err, e);
                }
            }

            if (entry == null) {
                entry = new CheckPointEntryVO(cpp.getCheckPointVO().getId(), name);
                dbf.persist(entry);
            }
        } catch (Exception e) {
            throw new CloudCheckPointException("[CheckPoint internal Error]", e);
        }

        if (ret == null) {
            /* Allow user exception to flee out here */
            ret = proceed(cp);
            
            try {
                checkPointEntrySuccess(entry, thisJoinPoint.getArgs(), ret, true);
            } catch (Exception e) {
                throw new CloudCheckPointException("[CheckPoint internal Error]", e);
            }
        } else {
            try {
                checkPointEntrySuccess(entry, null, null, false);
            } catch (Exception e) {
                throw new CloudCheckPointException("[CheckPoint internal Error]", e);
            }
        }

        return ret;
    }
}
