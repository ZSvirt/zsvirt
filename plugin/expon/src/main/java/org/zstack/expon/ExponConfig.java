package org.zstack.expon;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @example
 * {"pools":[{"name":"pool", "aliasName":"test"}]}
 */
public class ExponConfig {
    public static class Pool {
        private String name;
        private String aliasName;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAliasName() {
            return aliasName;
        }

        public void setAliasName(String aliasName) {
            this.aliasName = aliasName;
        }
    }

    private List<Pool> pools;

    public void setPools(List<Pool> pools) {
        this.pools = pools;
    }

    public List<Pool> getPools() {
        return pools;
    }

    public Set<String> getPoolNames() {
        return pools == null ? Collections.emptySet() : pools.stream().map(Pool::getName).collect(Collectors.toSet());
    }
}
