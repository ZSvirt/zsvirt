package org.zstack.testlib

class TestContext {
    private static ThreadLocal<Map<String, Object>> local = new ThreadLocal<Map<String, Object>>()

    private static void put(String k, Object v) {
        def map = local.get()
        if (map == null) {
            map = [:]
            local.set(map)
        }

        map[k] = v
    }

    private static Object get(String k) {
        def map = local.get()
        return map == null ? null : map[k]
    }

    private static void remove(String k) {
        def map = local.get()
        if (map != null) {
            map.remove(k)
        }
    }

    static final String SESSION_CLOSURE = "SESSION_CLOSURE"

    static void setSessionClosure(Closure c) {
        put(SESSION_CLOSURE, c)
    }

    static Closure getSesionClosure() {
        return get(SESSION_CLOSURE)
    }

    static void removeSessionClosure() {
        remove(SESSION_CLOSURE)
    }
}
