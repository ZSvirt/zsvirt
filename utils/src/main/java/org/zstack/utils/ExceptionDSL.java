package org.zstack.utils;

import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;

/**
 */
public class ExceptionDSL {
    private static final CLogger logger = Utils.getLogger(ExceptionDSL.class);

    public static class ExceptionWrapper {
        public ExceptionWrapper throwableSafe(RunnableWithThrowable runnable) {
            try {
                runnable.run();
            } catch (Throwable t) {
                logger.warn("unhandled throwable happened", t);
            }

            return this;
        }

        public ExceptionWrapper throwableSafe(RunnableWithThrowable runnable, String msg) {
            try {
                runnable.run();
            } catch (Throwable t) {
                logger.warn(String.format("%s, unhandled throwable happened", msg), t);
            }

            return this;
        }

        public ExceptionWrapper throwableSafeSuppress(RunnableWithThrowable runnable, Class<? extends Throwable>...tclazz) {
            try {
                runnable.run();
            } catch (Throwable t) {
                boolean suppress = false;
                for (Class<? extends Throwable> tc : tclazz) {
                    if (tc.isAssignableFrom(t.getClass())) {
                        suppress = true;
                        break;
                    }
                }

                if (!suppress) {
                    logger.warn(String.format("unhandled throwable happened"), t);
                }
            }

            return this;
        }

        public ExceptionWrapper throwableSafe(Runnable runnable) {
            try {
                runnable.run();
            } catch (Throwable t) {
                logger.warn("unhandled throwable happened", t);
            }

            return this;
        }

        public ExceptionWrapper throwableSafe(Runnable runnable, String msg) {
            try {
                runnable.run();
            } catch (Throwable t) {
                logger.warn(String.format("%s, unhandled throwable happened", msg), t);
            }

            return this;
        }

        public ExceptionWrapper throwableSafeSuppress(Runnable runnable, Class<? extends Throwable>...tclazz) {
            try {
                runnable.run();
            } catch (Throwable t) {
                boolean suppress = false;
                for (Class<? extends Throwable> tc : tclazz) {
                    if (tc.isAssignableFrom(t.getClass())) {
                        suppress = true;
                        break;
                    }
                }

                if (!suppress) {
                    logger.warn(String.format("unhandled throwable happened"), t);
                }
            }

            return this;
        }

        public ExceptionWrapper exceptionSafe(Runnable runnable) {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.warn("unhandled throwable happened", e);
            }
            return this;
        }

        public ExceptionWrapper exceptionSafe(Runnable runnable, String msg) {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.warn(String.format("%s, unhandled throwable happened", msg), e);
            }
            return this;
        }

        public ExceptionWrapper exceptionSafeSuppress(Runnable runnable, Class<? extends Exception>...eclazz) {
            try {
                runnable.run();
            } catch (Exception e) {
                boolean suppress = false;
                for (Class<? extends Exception> ec : eclazz) {
                    if (ec.isAssignableFrom(e.getClass())) {
                        suppress = true;
                        break;
                    }
                }

                if (!suppress) {
                    logger.warn(String.format("unhandled throwable happened"), e);
                }
            }
            return this;
        }
    }

    private static ExceptionWrapper self = new ExceptionWrapper();

    public static interface RunnableWithThrowable {
        void run() throws Throwable;
    }

    public static ExceptionWrapper throwableSafe(RunnableWithThrowable runnable) {
        return self.throwableSafe(runnable);
    }

    public static ExceptionWrapper throwableSafe(RunnableWithThrowable runnable, String msg) {
        return self.throwableSafe(runnable, msg);
    }

    public static ExceptionWrapper throwableSafeSuppress(RunnableWithThrowable runnable, Class<? extends Throwable>...tclazz) {
        return self.throwableSafeSuppress(runnable, tclazz);
    }

    public static ExceptionWrapper throwableSafe(Runnable runnable) {
        return self.throwableSafe(runnable);
    }

    public static ExceptionWrapper throwableSafe(Runnable runnable, String msg) {
        return self.throwableSafe(runnable, msg);
    }

    public static ExceptionWrapper throwableSafeSuppress(Runnable runnable, Class<? extends Throwable>...tclazz) {
        return self.throwableSafeSuppress(runnable, tclazz);
    }

    public static ExceptionWrapper exceptionSafe(Runnable runnable) {
        return self.exceptionSafe(runnable);
    }

    public static ExceptionWrapper exceptionSafe(Runnable runnable, String msg) {
        return self.exceptionSafe(runnable, msg);
    }

    public static ExceptionWrapper exceptionSafeSuppress(Runnable runnable, Class<? extends Exception>...eclazz) {
        return self.exceptionSafeSuppress(runnable, eclazz);
    }

    public static boolean isCausedBy(Throwable t, Class<? extends Throwable> causeClass) {
        return isCausedBy(t, causeClass, Collections.emptyList());
    }

    public static boolean isCausedBy(Throwable t, Class<? extends Throwable> causeClass, List<String> contents) {
        if (causeClass.isAssignableFrom(t.getClass())) {
            return true;
        }

        while (t.getCause() != null) {
            t = t.getCause();
            if (causeClass.isAssignableFrom(t.getClass())) {
                if (contents == null || contents.isEmpty() || t.getMessage() == null) {
                    return true;
                }

                for (String content : contents) {
                    if (!t.getMessage().contains(content)) {
                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }

    public static boolean isCausedBy(Throwable t, Class<? extends Throwable> causeClass, String content) {
        return isCausedBy(t, causeClass, Collections.singletonList(content));
    }

    public static Throwable getRootThrowable(Throwable t) {
        Throwable ret = t;
        while (t.getCause() != null) {
            t = ret = t.getCause();
        }
        return ret;
    }
}
