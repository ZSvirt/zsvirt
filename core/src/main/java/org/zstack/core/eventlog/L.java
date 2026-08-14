package org.zstack.core.eventlog;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L {
    private final static CLogger logger = Utils.getLogger(L.class);

    private final EventLogBuilder builder;

    @Autowired
    private EventLogManager mgr;

    public static L New(String category, Class<?> resourceClass, String resourceUuid) {
        return new L(category, resourceClass.getSimpleName(), resourceUuid);
    }

    private L() {
        builder = new EventLogBuilder();
    }

    private L(String category, String resourceType, String resourceUuid) {
        this();
        builder.category(category)
               .resource(resourceUuid, resourceType);
    }

    @ExceptionSafe
    private void save() {
        mgr.accept(builder);
    }

    public L trackingId(String trackingId) {
        builder.trackingId(trackingId);
        return this;
    }

    public void warn_(String fmt, Object...args) {
        builder.content(fmt).arguments(args).type(EventLogType.Warning);
        save();
        logger.warn(String.format(fmt, args));
    }

    public void info_(String fmt, Object...args) {
        builder.content(fmt).arguments(args).type(EventLogType.Info);
        save();
        logger.info(String.format(fmt, args));
    }

    public void error_(String fmt, Object...args) {
        builder.content(fmt).arguments(args).type(EventLogType.Error);
        save();
        logger.error(String.format(fmt, args));
    }
}
