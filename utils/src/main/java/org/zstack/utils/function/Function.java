package org.zstack.utils.function;

public interface Function<K, V> extends java.util.function.Function<V, K> {
    K call(V arg);

    @Override
    default K apply(V v) {
        return call(v);
    }
}
