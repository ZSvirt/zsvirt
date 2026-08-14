package org.zstack.header.errorcode;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
}
