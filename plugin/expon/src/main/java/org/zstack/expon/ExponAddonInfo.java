package org.zstack.expon;

import org.zstack.expon.sdk.cluster.TianshuClusterModule;
import org.zstack.expon.sdk.pool.FailureDomainModule;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ExponAddonInfo {
    private List<TianshuCluster> clusters = new ArrayList<>();

    private List<Pool> pools = new ArrayList<>();

    private String currentIscsiTargetId;
    private int currentIscsiTargetIndex;

    private String currentImageIscsiTargetId;
    private int currentImageIscsiTargetIndex;

    public List<Pool> getPools() {
        return pools;
    }

    public void setPools(List<Pool> pools) {
        this.pools = pools;
    }

    public void setClusters(List<TianshuCluster> clusters) {
        this.clusters = clusters;
    }

    public List<TianshuCluster> getClusters() {
        return clusters;
    }

    public String getCurrentIscsiTargetId() {
        return currentIscsiTargetId;
    }

    public void setCurrentIscsiTargetId(String currentIscsiTargetId) {
        this.currentIscsiTargetId = currentIscsiTargetId;
    }

    public int getCurrentIscsiTargetIndex() {
        return currentIscsiTargetIndex;
    }

    public void setCurrentIscsiTargetIndex(int currentIscsiTargetIndex) {
        this.currentIscsiTargetIndex = currentIscsiTargetIndex;
    }

    public String getCurrentImageIscsiTargetId() {
        return currentImageIscsiTargetId;
    }

    public void setCurrentImageIscsiTargetId(String currentImageIscsiTargetId) {
        this.currentImageIscsiTargetId = currentImageIscsiTargetId;
    }

    public int getCurrentImageIscsiTargetIndex() {
        return currentImageIscsiTargetIndex;
    }

    public void setCurrentImageIscsiTargetIndex(int currentImageIscsiTargetIndex) {
        this.currentImageIscsiTargetIndex = currentImageIscsiTargetIndex;
    }

    public static class Pool {
        private String id;
        private String name;
        private long availableCapacity;
        private long totalCapacity;
        private String tianshuId;
        private String healthStatus;
        private String redundancyPolicy;
        private int replicatedSize;
        private Timestamp createDate;
        private Timestamp lastOpDate;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getAvailableCapacity() {
            return availableCapacity;
        }

        public void setAvailableCapacity(long availableCapacity) {
            this.availableCapacity = availableCapacity;
        }

        public long getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(long totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public Timestamp getCreateDate() {
            return createDate;
        }

        public void setCreateDate(Timestamp createDate) {
            this.createDate = createDate;
        }

        public String getTianshuId() {
            return tianshuId;
        }

        public void setTianshuId(String tianshuId) {
            this.tianshuId = tianshuId;
        }

        public String getHealthStatus() {
            return healthStatus;
        }

        public void setHealthStatus(String healthStatus) {
            this.healthStatus = healthStatus;
        }

        public static Pool valueOf(FailureDomainModule failureDomainModule) {
            Pool pool = new Pool();
            pool.setId(failureDomainModule.getId());
            pool.setName(failureDomainModule.getFailureDomainName());
            pool.setAvailableCapacity(failureDomainModule.getAvailableCapacity());
            pool.setTotalCapacity(failureDomainModule.getValidSize());
            pool.setTianshuId(failureDomainModule.getTianshuId());
            pool.setHealthStatus(failureDomainModule.getHealthStatus());
            pool.setRedundancyPolicy(failureDomainModule.getRedundancyPloy());
            pool.setReplicatedSize(failureDomainModule.getReplicateSize());
            pool.setCreateDate(new Timestamp(failureDomainModule.getCreateTime()));
            pool.setLastOpDate(new Timestamp(failureDomainModule.getUpdateTime()));
            return pool;
        }

        public Timestamp getLastOpDate() {
            return lastOpDate;
        }

        public void setLastOpDate(Timestamp lastOpDate) {
            this.lastOpDate = lastOpDate;
        }

        public String getRedundancyPolicy() {
            return redundancyPolicy;
        }

        public void setRedundancyPolicy(String redundancyPolicy) {
            this.redundancyPolicy = redundancyPolicy;
        }

        public int getReplicatedSize() {
            return replicatedSize;
        }

        public void setReplicatedSize(int replicatedSize) {
            this.replicatedSize = replicatedSize;
        }
    }

    public static class TianshuCluster {
        private String id;
        private String name;
        private String healthStatus;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHealthStatus() {
            return healthStatus;
        }

        public void setHealthStatus(String healthStatus) {
            this.healthStatus = healthStatus;
        }

        public static TianshuCluster valueOf(TianshuClusterModule tianshuClusterModule) {
            TianshuCluster tianshuCluster = new TianshuCluster();
            tianshuCluster.setId(tianshuClusterModule.getId());
            tianshuCluster.setName(tianshuClusterModule.getName());
            tianshuCluster.setHealthStatus(tianshuClusterModule.getHealthStatus());
            return tianshuCluster;
        }
    }
}
