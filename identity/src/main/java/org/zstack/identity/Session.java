package org.zstack.identity;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

public class Session implements Component {
    private static final CLogger logger = Utils.getLogger(Session.class);

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;

    private Future<Void> expiredSessionCollector;
    private static Interner<String> sessionLock = Interners.newWeakInterner();

    private static Map<String, SessionInventory> sessions = new ConcurrentHashMap<>();

    public static SessionInventory loginByAdmin() {
        return login(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
    }

    public static SessionInventory login(String accountUuid) {
        if (IdentityGlobalConfig.ENABLE_UNIQUE_SESSION.value(Boolean.class)) {
            List<String> currentSessionUuids = Q.New(SessionVO.class)
                    .select(SessionVO_.uuid)
                    .eq(SessionVO_.accountUuid, accountUuid)
                    .listValues();
            IdentityCanonicalEvents.SessionForceLogoutData data = new IdentityCanonicalEvents.SessionForceLogoutData();
            data.setAccountUuid(accountUuid);

            PluginRegistry pluginRgty = getComponentLoader().getComponent(PluginRegistry.class);
            EventFacade evtf = getComponentLoader().getComponent(EventFacade.class);
            for (String sessionUuid : currentSessionUuids) {
                logout(sessionUuid);
                data.setSessionUuid(sessionUuid);

                evtf.fire(IdentityCanonicalEvents.SESSION_FORCE_LOGOUT_PATH, data);
                for (ForceLogoutSessionExtensionPoint ext : pluginRgty.getExtensionList(ForceLogoutSessionExtensionPoint.class)) {
                    ext.afterForceLogoutSession(data);
                }
            }
        }

        return new SQLBatchWithReturn<SessionInventory>() {
            @Transactional(readOnly = true)
            private Timestamp getCurrentSqlDate() {
                Query query = databaseFacade.getEntityManager().createNativeQuery("select current_timestamp()");
                return (Timestamp) query.getSingleResult();
            }

            @Override
            protected SessionInventory scripts() {
                if (q(SessionVO.class).eq(SessionVO_.accountUuid, accountUuid).count() >= IdentityGlobalConfig.MAX_CONCURRENT_SESSION.value(Integer.class)) {
                    throw new OperationFailureException(err(IdentityErrors.MAX_CONCURRENT_SESSION_EXCEEDED, "Login sessions hit limit of max allowed concurrent login sessions"));
                }

                SessionVO vo = new SessionVO();
                vo.setUuid(Platform.getUuid());
                vo.setAccountUuid(accountUuid);
                long expiredTime = getCurrentSqlDate().getTime() + TimeUnit.SECONDS.toMillis(IdentityGlobalConfig.SESSION_TIMEOUT.value(Long.class));
                vo.setExpiredDate(new Timestamp(expiredTime));
                persist(vo);
                reload(vo);
                SessionInventory inv = SessionInventory.valueOf(vo);
                sessions.put(inv.getUuid(), inv);

                return inv;
            }
        }.execute();
    }

    public static SessionInventory renewSession(String uuid, Long extendPeriod) {
        errorOnTimeout(uuid);

        if (extendPeriod == null) {
            extendPeriod = IdentityGlobalConfig.SESSION_TIMEOUT.value(Long.class);
        }

        Long finalExtendPeriod = extendPeriod;
        SessionInventory session = new SQLBatchWithReturn<SessionInventory>() {
            @Transactional(readOnly = true)
            private Timestamp getCurrentSqlDate() {
                Query query = databaseFacade.getEntityManager().createNativeQuery("select current_timestamp()");
                return (Timestamp) query.getSingleResult();
            }

            @Override
            protected SessionInventory scripts() {
                Timestamp expiredDate = new Timestamp(TimeUnit.SECONDS.toMillis(finalExtendPeriod) + getCurrentSqlDate().getTime());
                SessionInventory s = getSession(uuid);
                if (s != null) {
                    s.setExpiredDate(expiredDate);
                    sql(SessionVO.class).eq(SessionVO_.uuid, uuid).set(SessionVO_.expiredDate, expiredDate).update();
                }

                return s;
            }
        }.execute();

        return session;
    }

    public static void logoutAccount(String accountUuid) {
        long count = SQL.New("select count(1) from SessionVO session " +
                "where session.accountUuid =:accountUuid", Long.class)
                .param("accountUuid", accountUuid).find();
        if (count != 0) {
            SQL.New("select session.uuid from SessionVO session " +
                    "where session.accountUuid =:accountUuid", String.class)
                    .param("accountUuid", accountUuid).limit(1000).skipIncreaseOffset(true).paginate(count,  (List<String> sessionUuids) -> {
                for (String sessionUuid : sessionUuids) {
                    Session.logout(sessionUuid);
                }
            });
        }
    }

    public static void logout(String uuid) {
        synchronized (sessionLock.intern(uuid)) {
            deleteSession(uuid);
        }
    }
    
    private static void deleteSession(String uuid) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                SessionInventory s = sessions.remove(uuid);
                if (s == null) {
                    SessionVO vo = findByUuid(uuid, SessionVO.class);
                    s = vo == null ? null : SessionInventory.valueOf(vo);
                }

                if (s == null) {
                    return;
                }

                SessionInventory finalS = s;
                getComponentLoader().getComponent(PluginRegistry.class)
                        .getExtensionList(SessionLogoutExtensionPoint.class)
                        .forEach(ext -> ext.sessionLogout(finalS));

                sql(SessionVO.class).eq(SessionVO_.uuid, uuid).hardDelete();
            }
        }.execute();
    }

    /**
     * Check if session which matches specific uuid is expired.
     * Validate the session store in cache first. if it is expired,
     * remove it from cache. And then check the expired date in db,
     * if it is expired, logout the session (delete db record)
     * @param uuid uuid of a session
     * @return if session is expired, return an error code, else return null
     */
    public static ErrorCode checkSessionExpired(String uuid) {
        return new SQLBatchWithReturn<ErrorCode>() {
            @Transactional(readOnly = true)
            private Timestamp getCurrentSqlDate() {
                Query query = databaseFacade.getEntityManager().createNativeQuery("select current_timestamp()");
                return (Timestamp) query.getSingleResult();
            }

            @Override
            protected ErrorCode scripts() {
                SessionInventory s = getSession(uuid);
                if (s == null) {
                    return err(IdentityErrors.INVALID_SESSION, "Session expired");
                }

                Timestamp curr = getCurrentSqlDate();
                if (curr.after(s.getExpiredDate())) {
                    if (logger.isTraceEnabled()) {
                        logger.debug(String.format("session expired[%s < %s] for account[uuid:%s] in cache", curr,
                                s.getExpiredDate(), s.getAccountUuid()));
                    }

                    SessionVO vo = findByUuid(uuid, SessionVO.class);
                    if (vo != null && curr.before(vo.getExpiredDate())) {
                        logger.debug(String.format("session not expired[%s < %s] for account[uuid:%s] in DB, just remove it from session cache", curr,
                                s.getExpiredDate(), s.getAccountUuid()));
                        sessions.remove(uuid);
                        return null;
                    }

                    logout(s.getUuid());
                    return err(IdentityErrors.INVALID_SESSION, "Session expired");
                }

                return null;
            }
        }.execute();
    }

    public static void errorOnTimeout(String uuid) {
        ErrorCode errorCode = checkSessionExpired(uuid);

        if (errorCode == null) {
            return;
        }

        throw new OperationFailureException(errorCode);
    }

    public static Map<String, SessionInventory> getSessionsCopy() {
        return new HashMap<>(sessions);
    }

    public static SessionInventory getSession(String uuid) {
        synchronized (sessionLock.intern(uuid)) {
            SessionInventory s = sessions.get(uuid);
            if (s != null) {
                return s;
            }

            SessionVO vo = Q.New(SessionVO.class).eq(SessionVO_.uuid, uuid).find();
            if (vo == null) {
                return null;
            }

            s = SessionInventory.valueOf(vo);
            sessions.put(s.getUuid(), s);
            return s;
        }
    }

    @Override
    public boolean start() {
        setupGlobalConfig();
        startCleanUpStaleSessionTask();
        setupCanonicalEvents();
        return true;
    }

    private void setupGlobalConfig() {
        IdentityGlobalConfig.SESSION_CLEANUP_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startCleanUpStaleSessionTask());
    }

    private synchronized void startCleanUpStaleSessionTask() {
        if (expiredSessionCollector != null) {
            expiredSessionCollector.cancel(true);
        }

        expiredSessionCollector = thdf.submitPeriodicTask(new PeriodicTask() {
            @Transactional
            private List<String> deleteExpiredSessions() {
                String sql = "select s.uuid from SessionVO s where CURRENT_TIMESTAMP  >= s.expiredDate";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                List<String> uuids = q.getResultList();
                if (!uuids.isEmpty()) {
                    String dsql = "delete from SessionVO s where s.uuid in :uuids";
                    Query dq = dbf.getEntityManager().createQuery(dsql);
                    dq.setParameter("uuids", uuids);
                    dq.executeUpdate();
                }
                return uuids;
            }

            @Transactional(readOnly = true)
            private Timestamp getCurrentSqlDate() {
                Query query = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
                return (Timestamp) query.getSingleResult();
            }

            private void deleteExpiredCachedSessions() {
                Timestamp curr = getCurrentSqlDate();
                List<String> staleSessionUuidInCache = sessions.entrySet().stream().filter(entry -> curr.after(entry.getValue().getExpiredDate())).map(entry -> entry.getKey()).collect(Collectors.toList());

                for (String uuid : staleSessionUuidInCache) {
                    logger.debug(String.format("found session[uuid:%s] in cache expired, remove it", uuid));
                    sessions.remove(uuid);
                }
            }

            @Override
            public void run() {
                List<String> uuids = deleteExpiredSessions();
                for (String uuid : uuids) {
                    logger.debug(String.format("found session[uuid:%s] expired in DB, also remove it from cache", uuid));
                    sessions.remove(uuid);
                }

                deleteExpiredCachedSessions();
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return IdentityGlobalConfig.SESSION_CLEANUP_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "ExpiredSessionCleanupThread";
            }

        });
    }

    @Override
    public boolean stop() {
        if (expiredSessionCollector != null) {
            expiredSessionCollector.cancel(true);
        }

        return true;
    }

    private void setupCanonicalEvents() {
        evtf.on(IdentityCanonicalEvents.ACCOUNT_DELETED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                // as a foreign key would clean SessionVO after account deleted, just clean memory sessions here
                removeMemorySessionsAccordingToDB(tokens, data);
                removeMemorySessionsAccordingToAccountUuid(tokens, data);
            }

            private void removeMemorySessionsAccordingToDB(Map tokens, Object data) {
                IdentityCanonicalEvents.AccountDeletedData d = (IdentityCanonicalEvents.AccountDeletedData) data;

                SimpleQuery<SessionVO> q = dbf.createQuery(SessionVO.class);
                q.select(SessionVO_.uuid);
                q.add(SessionVO_.accountUuid, SimpleQuery.Op.EQ, d.getAccountUuid());
                List<String> suuids = q.listValue();

                for (String uuid : suuids) {
                    logout(uuid);
                }

                if (!suuids.isEmpty()) {
                    logger.debug(String.format("successfully removed %s sessions for the deleted account[%s]",
                            suuids.size(),
                            d.getAccountUuid()));
                }
            }

            private void removeMemorySessionsAccordingToAccountUuid(Map tokens, Object data) {
                IdentityCanonicalEvents.AccountDeletedData d = (IdentityCanonicalEvents.AccountDeletedData) data;

                List<String> suuids = sessions.entrySet().stream()
                        .filter(it -> it.getValue().getAccountUuid().equals(d.getAccountUuid()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                for (String uuid : suuids) {
                    logout(uuid);
                }

                if (!suuids.isEmpty()) {
                    logger.debug(String.format("successfully removed %s sessions for the deleted account[%s]",
                            suuids.size(),
                            d.getAccountUuid()));
                }
            }
        });
    }
}
