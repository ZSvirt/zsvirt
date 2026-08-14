package org.zstack.utils;

import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 */
public class CollectionUtils {
    private static final CLogger logger = Utils.getLogger(CollectionUtils.class);

    public static <T> List<T> getDuplicateElementsOfList(List<T> list) {
        List<T> result = new ArrayList<T>();
        Set<T> set = new HashSet<T>();
        for (T e : list) {
            if (!set.add(e)) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * use {@link #transformAndRemoveNull(Collection, java.util.function.Function)}
     */
    @Deprecated
    public static <K, V> List<K> transformToList(Collection<V> from, Function<K, V> func) {
        List<K> ret = new ArrayList<K>();
        for (V v : from) {
            K k = func.call(v);
            if (k == null) {
                continue;
            }
            ret.add(k);
        }

        return ret;
    }

    public static <FROM, TO> List<TO> transform(Collection<FROM> from, java.util.function.Function<FROM, TO> mapper) {
        return from.stream().map(mapper).collect(Collectors.toList());
    }

    public static <FROM, TO> List<TO> transformAndRemoveNull(Collection<FROM> from, java.util.function.Function<FROM, TO> mapper) {
        return from.stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static <T> List<T> filter(Collection<T> from, Predicate<T> tester) {
        return from.stream().filter(tester).collect(Collectors.toList());
    }

    @Nullable
    public static <T> T findOneOrNull(Collection<T> from, Predicate<T> tester) {
        return from.stream().filter(tester).findFirst().orElse(null);
    }

    public static <FROM, KEY, VALUE> Map<KEY, VALUE> toMap(Collection<FROM> from,
                                                           java.util.function.Function<FROM, KEY> keyMapper,
                                                           java.util.function.Function<FROM, VALUE> valueMapper) {
        return from.stream().collect(Collectors.toMap(keyMapper, valueMapper));
    }

    public static <KEY, VALUE> Map<KEY, List<VALUE>> groupBy(Collection<VALUE> from,
                                                             java.util.function.Function<VALUE, KEY> keyMapper) {
        return from.stream().collect(Collectors.groupingBy(keyMapper));
    }

    public static <FROM, TO> Set<TO> transformToSetAndRemoveNull(Collection<FROM> from,
                                                               java.util.function.Function<FROM, TO> mapper) {
        return from.stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public static <FROM, TO> Set<TO> transformToSet(Collection<FROM> from, java.util.function.Function<FROM, TO> mapper) {
        return from.stream().map(mapper).collect(Collectors.toSet());
    }

    /**
     * use {@link #findOneOrNull}
     */
    @Deprecated
    public static <K, V> K find(Collection<V> from, Function<K, V> func) {
        for (V v : from) {
            K k = func.call(v);
            if (k != null) {
                return k;
            }
        }

        return null;
    }

    public static <K> void forEach(Collection<K> cols, ForEachFunction<K> func) {
        for (K c : cols) {
            func.run(c);
        }
    }

    public static <K> void safeForEach(Collection<K> cols, ForEachFunction<K> func) {
        for (K c : cols) {
            try {
                func.run(c);
            } catch (Throwable t) {
                logger.warn("unhandled exception happened", t);
            }
        }
    }

    public static <K> List<K> removeDuplicateFromList(List<K> lst) {
        return new ArrayList<K>(new LinkedHashSet<K>(lst));
    }

    public static <T> Predicate<T> distinctByKey(java.util.function.Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @SuppressWarnings("unchecked")
    public static <T> void shuffleByKeySeed(List<T> list, String keySeed, java.util.function.Function<? super T, Comparable> keyExtractor) {
        list.sort(Comparator.comparing(keyExtractor));

        long seed = 0;
        char[] keyArrary = keySeed.toCharArray();
        for (char c : keyArrary) {
            seed += c;
        }

        Random r = new Random(seed);
        List<T> result = new ArrayList<>();
        for (int i = list.size(); i > 0; i--) {
            result.add(list.remove(r.nextInt(i)));
        }
        list.addAll(result);
    }

    public static <T> List<T> merge(List<T> listA, List<T> listB) {
        listA = org.apache.commons.collections.CollectionUtils.isNotEmpty(listA) ? listA : new ArrayList<T>();
        listB = org.apache.commons.collections.CollectionUtils.isNotEmpty(listB) ? listB : new ArrayList<T>();
        List<T> list = new ArrayList<T>();
        list.addAll(listA);
        list.addAll(listB);
        return list;
    }

    public static boolean isEmpty(Collection coll) {
        return coll == null || coll.isEmpty();
    }

    @SafeVarargs
    public static <T> boolean contains(T[] array, T... items) {
        for (T a: array) {
            for (T b: items) {
                if (Objects.equals(a, b)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Stream<String> valuesForEnums(Class<? extends Enum<?>>[] enumClasses) {
        return Arrays.stream(enumClasses)
                    .flatMap(CollectionUtils::valuesForEnum)
                    .distinct();
    }

    public static Stream<String> valuesForEnum(Class<? extends Enum<?>> enumClass) {
        try {
            final Method method = enumClass.getMethod("values");
            final Object[] strings = (Object[]) method.invoke(null);
            return Arrays.stream(strings).map(Object::toString);
        } catch (Exception e) {
            throw new RuntimeException("failed to parse valid values from enumClass: " + enumClass, e);
        }
    }
}
