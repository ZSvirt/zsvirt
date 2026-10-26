package org.zstack.zmigrate.compute;

import org.zstack.zmigrate.ZMigrateVddkBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class ZMigrateVddkDistribution {
    public interface Gateway {
        Set<String> queryVersions(long timeoutInMillis);
        void upload(String file);
    }

    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }

    private final Gateway gateway;
    private final long timeoutInMillis;
    private final long intervalInMillis;
    private final LongSupplier clock;
    private final Sleeper sleeper;

    public ZMigrateVddkDistribution(Gateway gateway) {
        this(gateway, TimeUnit.MINUTES.toMillis(5), TimeUnit.SECONDS.toMillis(3),
                () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime()), Thread::sleep);
    }

    ZMigrateVddkDistribution(Gateway gateway, long timeoutInMillis, long intervalInMillis,
                            LongSupplier clock, Sleeper sleeper) {
        this.gateway = gateway;
        this.timeoutInMillis = timeoutInMillis;
        this.intervalInMillis = intervalInMillis;
        this.clock = clock;
        this.sleeper = sleeper;
    }

    public void distribute(Map<String, String> files) {
        Set<String> installed = gateway.queryVersions(timeoutInMillis);
        for (String version : ZMigrateVddkBundle.getDistributionVersions(files)) {
            if (isInstalled(installed, version)) {
                continue;
            }
            String file = files.get(version);
            gateway.upload(file);
            installed = waitForVersion(version, file);
        }
        List<String> missing = new ArrayList<>();
        for (String version : files.keySet()) {
            if (!isInstalled(installed, version)) {
                missing.add(version);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("VDDK versions missing after distribution: " + missing);
        }
    }

    private Set<String> waitForVersion(String version, String file) {
        long started = clock.getAsLong();
        RuntimeException lastQueryFailure = null;
        long budget;
        while ((budget = remaining(started)) > 0) {
            try {
                Set<String> installed = gateway.queryVersions(budget);
                if (remaining(started) <= 0) {
                    break;
                }
                if (isInstalled(installed, version)) {
                    return installed;
                }
            } catch (RuntimeException failure) {
                lastQueryFailure = failure;
            }
            long remaining = remaining(started);
            if (remaining <= 0) {
                break;
            }
            try {
                sleeper.sleep(Math.min(intervalInMillis, remaining));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted confirming VDDK[version:" + version
                        + ",file:" + file + "]", interrupted);
            }
        }
        throw new IllegalStateException("timed out confirming VDDK[version:" + version
                + ",file:" + file + "]", lastQueryFailure);
    }

    private long remaining(long started) {
        return timeoutInMillis - (clock.getAsLong() - started);
    }

    private boolean isInstalled(Set<String> installed, String version) {
        return installed.contains(version)
                || ("6.7.3".equals(version) && installed.contains("6.7.0"));
    }
}
