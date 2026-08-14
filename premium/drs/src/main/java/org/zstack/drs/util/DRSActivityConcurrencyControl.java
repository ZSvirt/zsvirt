package org.zstack.drs.util;

import java.util.*;

/**
 * Created by lining on 2018/10/18.
 */
public class DRSActivityConcurrencyControl {
    private static Set<String> tokens = Collections.synchronizedSet(new HashSet<>());
    private static Map<String, LockObject> lockObjectPool = Collections.synchronizedMap(new HashMap<>());

    public static boolean addToken(String token) {
        LockObject lock = lockObjectPool.get(token);
        if (lock == null) {
            lock = buildLockString(token);
        }

        synchronized (lock) {
            if (tokens.contains(token)) {
                return false;
            }

            tokens.add(token);
            return true;
        }
    }

    public static void removeToken(String token) {
        LockObject lock = lockObjectPool.get(token);
        if (lock == null) {
            lock = buildLockString(token);
        }

        synchronized (lock) {
            tokens.remove(token);
            lockObjectPool.remove(token);
        }
    }

    public static boolean isExists(String token) {
        LockObject lock = lockObjectPool.get(token);
        if (lock == null) {
            lock = buildLockString(token);
        }

        synchronized (lock) {
            if (tokens.contains(token)) {
                return true;
            }

            return false;
        }
    }

    private static synchronized LockObject buildLockString(String token) {
        LockObject lockObject = lockObjectPool.get(token);

        if (lockObject == null) {
            lockObject = new LockObject(token);
            lockObjectPool.put(token, lockObject);
            return lockObject;
        }

        return lockObject;
    }

    static class LockObject {
        String token;

        LockObject(String token) {
            this.token = token;
        }
    }
}


