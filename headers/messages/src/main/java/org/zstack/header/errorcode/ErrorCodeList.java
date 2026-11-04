package org.zstack.header.errorcode;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class ErrorCodeList implements Iterable<ErrorCode> {
    protected final List<ErrorCode> causes = Collections.synchronizedList(new ArrayList<>());

    public List<ErrorCode> getCauses() {
        return causes;
    }

    public ErrorCodeList add(ErrorCode cause) {
        causes.add(cause);
        return this;
    }

    public boolean hasError() {
        return !causes.isEmpty();
    }

    public boolean isEmpty() {
        return causes.isEmpty();
    }

    public int size() {
        return causes.size();
    }

    @NotNull
    @Override
    public Iterator<ErrorCode> iterator() {
        return causes.listIterator();
    }

    public ErrorCode wrapOrNull() {
        synchronized (causes) {
            if (causes.isEmpty()) {
                return null;
            }

            if (causes.size() == 1) {
                return causes.get(0);
            }

            return multiErrorCode(causes);
        }
    }

    // ------------------------------------- To ErrorCode ------------------------------------

    private static Function<Collection<ErrorCode>, ErrorCode> multiErrorGenerator;
    public static void putMultiErrorGenerator(Function<Collection<ErrorCode>, ErrorCode> generator) {
        if (multiErrorGenerator == null) {
            multiErrorGenerator = generator;
        } else {
            throw new IllegalStateException("multiErrorGenerator already been placed");
        }
    }

    private static ErrorCode multiErrorCode(Collection<ErrorCode> list) {
        if (multiErrorGenerator != null) {
            try {
                return multiErrorGenerator.apply(list);
            } catch (RuntimeException ignored) {} // no logger in headers module
        }

        ErrorCode multi = new ErrorCode().withCause(list); // no i18n
        multi.setCode(SysErrors.MULTIPLE_REASONS.toString());
        multi.setDescription("Multiple errors");
        multi.setDetails("More than one errors occurs. See more details in causes");
        return multi;
    }
}
