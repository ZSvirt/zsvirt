package org.zstack.header.vm.metadata;

import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Records the metadata flush state for each VM instance, and tracks
 * the stale-recovery lifecycle when a flush fails permanently.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Normal flush success: {@code metadataSnapshot} is updated,
 *       {@code pendingStaleRecovery} and {@code staleRecoveryCount} are reset to 0.</li>
 *   <li>Flush failure (retry exhausted): {@code pendingStaleRecovery} is set to {@code true},
 *       the dirty row is deleted, and the VM enters the stale-recovery queue.</li>
 *   <li>Stale recovery task picks up the VM, calls {@code markDirty} to re-enter
 *       the flush flow, and increments {@code staleRecoveryCount}.</li>
 *   <li>Circuit breaker: when {@code staleRecoveryCount >= maxCycles},
 *       both fields are reset to 0 (permanent-stale), requiring manual intervention
 *       via {@code APIUpdateVmInstanceMetadataMsg}.</li>
 * </ol>
 *
 * <h3>Content drift detection</h3>
 * A periodic task compares live-computed metadata against {@code metadataSnapshot}.
 * If they differ, the VM is marked dirty for re-flush.
 */
@Entity
@Table
public class VmMetadataFlushStateVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    /**
     * Timestamp when the last successful flush completed.
     * Null if the VM has never been flushed.
     */
    @Column
    private Timestamp lastFlushFinishTime;

    /**
     * Whether this VM is waiting for the stale-recovery periodic task to
     * re-queue it into the flush pipeline. Set to {@code true} when flush
     * retry is exhausted; cleared on next successful flush or circuit break.
     */
    @Column
    private boolean pendingStaleRecovery;

    /**
     * Number of stale-recovery cycles attempted so far.
     * Incremented each time the recovery task re-queues this VM.
     * When it reaches {@code maxCycles}, the circuit breaker fires
     * and resets both this field and {@code pendingStaleRecovery} to 0.
     */
    @Column
    private int staleRecoveryCount;

    /**
     * JSON snapshot of the VM metadata written during the last successful flush.
     * Used by the content-drift detector to compare against live-computed metadata.
     */
    @Column
    @Lob
    private String metadataSnapshot;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public Timestamp getLastFlushFinishTime() {
        return lastFlushFinishTime;
    }

    public void setLastFlushFinishTime(Timestamp lastFlushFinishTime) {
        this.lastFlushFinishTime = lastFlushFinishTime;
    }

    public boolean isPendingStaleRecovery() {
        return pendingStaleRecovery;
    }

    public void setPendingStaleRecovery(boolean pendingStaleRecovery) {
        this.pendingStaleRecovery = pendingStaleRecovery;
    }

    public int getStaleRecoveryCount() {
        return staleRecoveryCount;
    }

    public void setStaleRecoveryCount(int staleRecoveryCount) {
        this.staleRecoveryCount = staleRecoveryCount;
    }

    public String getMetadataSnapshot() {
        return metadataSnapshot;
    }

    public void setMetadataSnapshot(String metadataSnapshot) {
        this.metadataSnapshot = metadataSnapshot;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
