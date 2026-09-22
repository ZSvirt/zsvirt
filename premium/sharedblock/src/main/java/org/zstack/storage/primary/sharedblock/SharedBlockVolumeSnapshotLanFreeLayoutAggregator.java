package org.zstack.storage.primary.sharedblock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class SharedBlockVolumeSnapshotLanFreeLayoutAggregator {
    private SharedBlockVolumeSnapshotLanFreeLayoutAggregator() {
    }

    static Result aggregate(
            List<SharedBlockVolumeSnapshotLanFreeTarget> targets,
            List<GetSharedBlockVolumeSnapshotLanFreeLayoutReply> replies) {
        if (targets == null || replies == null) {
            throw new IllegalArgumentException(
                    "targets and backend replies cannot be null");
        }

        Map<String, SharedBlockVolumeSnapshotLanFreeTarget> targetsByUuid =
                new HashMap<>();
        for (SharedBlockVolumeSnapshotLanFreeTarget target : targets) {
            if (target == null || isBlank(target.getVolumeSnapshotUuid())
                    || target.getRequestIndex() == null) {
                throw new IllegalArgumentException(
                        "invalid Volume Snapshot LAN-free target");
            }
            if (targetsByUuid.put(
                    target.getVolumeSnapshotUuid(), target) != null) {
                throw new IllegalArgumentException(String.format(
                        "duplicate Volume Snapshot LAN-free target[%s]",
                        target.getVolumeSnapshotUuid()));
            }
        }

        Map<String, SharedBlockLunDescriptor> lunsByWwid =
                new LinkedHashMap<>();
        Map<String, SharedBlockVolumeSnapshotLanFreeLayout> layoutsByUuid =
                new HashMap<>();
        for (GetSharedBlockVolumeSnapshotLanFreeLayoutReply reply : replies) {
            if (reply == null || reply.getLuns() == null
                    || reply.getLayouts() == null) {
                throw new IllegalArgumentException(
                        "SharedBlock backend returned null LAN-free LUNs or layouts");
            }
            mergeLuns(lunsByWwid, reply.getLuns());
            for (SharedBlockVolumeSnapshotLanFreeLayout layout
                    : reply.getLayouts()) {
                if (layout == null || isBlank(layout.getVolumeSnapshotUuid())) {
                    throw new IllegalArgumentException(
                            "SharedBlock backend returned an invalid Volume Snapshot LAN-free layout");
                }
                if (layoutsByUuid.put(
                        layout.getVolumeSnapshotUuid(), layout) != null) {
                    throw new IllegalArgumentException(String.format(
                            "SharedBlock backend returned duplicate Volume Snapshot LAN-free layout[%s]",
                            layout.getVolumeSnapshotUuid()));
                }
            }
        }

        if (!layoutsByUuid.keySet().equals(targetsByUuid.keySet())) {
            Set<String> missing = new LinkedHashSet<>(targetsByUuid.keySet());
            missing.removeAll(layoutsByUuid.keySet());
            Set<String> unexpected = new LinkedHashSet<>(layoutsByUuid.keySet());
            unexpected.removeAll(targetsByUuid.keySet());
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned mismatched Volume Snapshot LAN-free layouts, missing%s, unexpected%s",
                    missing, unexpected));
        }

        Set<String> referencedWwids = new HashSet<>();
        for (Map.Entry<String, SharedBlockVolumeSnapshotLanFreeLayout> entry
                : layoutsByUuid.entrySet()) {
            validateLayout(
                    targetsByUuid.get(entry.getKey()), entry.getValue(),
                    lunsByWwid, referencedWwids);
        }

        Set<String> unreferencedWwids =
                new LinkedHashSet<>(lunsByWwid.keySet());
        unreferencedWwids.removeAll(referencedWwids);
        if (!unreferencedWwids.isEmpty()) {
            throw new IllegalArgumentException(
                    "SharedBlock backend returned LUNs not referenced by any layer: "
                            + unreferencedWwids);
        }

        List<SharedBlockVolumeSnapshotLanFreeLayout> orderedLayouts =
                targets.stream()
                        .sorted(Comparator.comparing(
                                SharedBlockVolumeSnapshotLanFreeTarget::getRequestIndex))
                        .map(target -> layoutsByUuid.get(
                                target.getVolumeSnapshotUuid()))
                        .collect(Collectors.toList());
        return new Result(
                new ArrayList<>(lunsByWwid.values()), orderedLayouts);
    }

    private static void mergeLuns(
            Map<String, SharedBlockLunDescriptor> lunsByWwid,
            List<SharedBlockLunDescriptor> luns) {
        for (SharedBlockLunDescriptor lun : luns) {
            validateLun(lun);
            SharedBlockLunDescriptor existing = lunsByWwid.get(lun.getWwid());
            if (existing != null) {
                if (!existing.getCapacityBytes().equals(lun.getCapacityBytes())) {
                    throw new IllegalArgumentException(String.format(
                            "SharedBlock backend returned conflicting capacities for LUN[%s]",
                            lun.getWwid()));
                }
                continue;
            }
            lunsByWwid.put(lun.getWwid(), lun);
        }
    }

    private static void validateLayout(
            SharedBlockVolumeSnapshotLanFreeTarget target,
            SharedBlockVolumeSnapshotLanFreeLayout layout,
            Map<String, SharedBlockLunDescriptor> lunsByWwid,
            Set<String> referencedWwids) {
        if (!Objects.equals(
                target.getVolumeUuid(), layout.getVolumeUuid())) {
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned mismatched identity for snapshot[%s]",
                    target.getVolumeSnapshotUuid()));
        }
        if (layout.getVirtualSizeBytes() == null
                || layout.getVirtualSizeBytes() <= 0) {
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned invalid virtual size for snapshot[%s]",
                    target.getVolumeSnapshotUuid()));
        }
        if (layout.getLayers() == null || layout.getLayers().isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned no layers for snapshot[%s]",
                    target.getVolumeSnapshotUuid()));
        }
        for (int index = 0; index < layout.getLayers().size(); index++) {
            SharedBlockVolumeSnapshotLanFreeLayer layer =
                    layout.getLayers().get(index);
            validateLayerIdentity(target, layout, layer, index);
            validateLayerRanges(
                    target, layer, lunsByWwid, referencedWwids);
        }
        if (!Objects.equals(
                target.getVolumeSnapshotInstallPath(),
                layout.getLayers().get(0).getSourceInstallPath())) {
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned a chain head that does not match snapshot[%s]",
                    target.getVolumeSnapshotUuid()));
        }
    }

    private static void validateLayerIdentity(
            SharedBlockVolumeSnapshotLanFreeTarget target,
            SharedBlockVolumeSnapshotLanFreeLayout layout,
            SharedBlockVolumeSnapshotLanFreeLayer layer,
            int index) {
        if (layer == null || layer.getLayerIndex() == null
                || layer.getLayerIndex() != index) {
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned non-contiguous layer index for snapshot[%s]",
                    target.getVolumeSnapshotUuid()));
        }
        if (isBlank(layer.getSourceInstallPath())
                || !isValidSourceInstallPath(
                target.getPrimaryStorageUuid(), layer.getSourceInstallPath())) {
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned invalid sourceInstallPath[%s] for snapshot[%s] layer[%s]",
                    layer.getSourceInstallPath(),
                    target.getVolumeSnapshotUuid(), index));
        }
        if (isBlank(layer.getFormat()) || layer.getLvSizeBytes() == null
                || layer.getLvSizeBytes() <= 0) {
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned invalid layer metadata for snapshot[%s]",
                    target.getVolumeSnapshotUuid()));
        }

        boolean last = index + 1 == layout.getLayers().size();
        if (!last) {
            if (!Integer.valueOf(index + 1).equals(
                    layer.getParentLayerIndex())) {
                throw new IllegalArgumentException(String.format(
                        "SharedBlock backend returned invalid parent layer for snapshot[%s] layer[%s]",
                        target.getVolumeSnapshotUuid(), index));
            }
            if (!"qcow2".equals(layer.getFormat())) {
                throw new IllegalArgumentException(String.format(
                        "SharedBlock backend returned non-qcow2 child layer for snapshot[%s] layer[%s]",
                        target.getVolumeSnapshotUuid(), index));
            }
        } else if (layer.getParentLayerIndex() != null) {
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned a parentLayerIndex outside response for snapshot[%s]",
                    target.getVolumeSnapshotUuid()));
        }
        if (!"qcow2".equals(layer.getFormat())
                && !"raw".equals(layer.getFormat())) {
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned unsupported layer format[%s] for snapshot[%s] layer[%s]",
                    layer.getFormat(), target.getVolumeSnapshotUuid(), index));
        }
    }

    private static void validateLayerRanges(
            SharedBlockVolumeSnapshotLanFreeTarget target,
            SharedBlockVolumeSnapshotLanFreeLayer layer,
            Map<String, SharedBlockLunDescriptor> lunsByWwid,
            Set<String> referencedWwids) {
        if (layer.getRanges() == null || layer.getRanges().isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned no ranges for snapshot[%s] layer[%s]",
                    target.getVolumeSnapshotUuid(), layer.getLayerIndex()));
        }

        long expectedOffset = 0L;
        for (SharedBlockLvRange range : layer.getRanges()) {
            if (range == null || isBlank(range.getWwid())
                    || range.getLvOffsetBytes() == null
                    || range.getLunOffsetBytes() == null
                    || range.getLengthBytes() == null
                    || range.getLvOffsetBytes() < 0
                    || range.getLunOffsetBytes() < 0
                    || range.getLengthBytes() <= 0
                    || range.getLvOffsetBytes() != expectedOffset) {
                throw new IllegalArgumentException(String.format(
                        "SharedBlock backend returned invalid range at LV offset[%s] for snapshot[%s] layer[%s]",
                        expectedOffset, target.getVolumeSnapshotUuid(),
                        layer.getLayerIndex()));
            }

            try {
                expectedOffset = Math.addExact(
                        expectedOffset, range.getLengthBytes());
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(
                        "SharedBlock backend returned LV range overflow");
            }
            SharedBlockLunDescriptor lun = lunsByWwid.get(range.getWwid());
            if (lun == null) {
                throw new IllegalArgumentException(String.format(
                        "SharedBlock backend range references unknown LUN[%s]",
                        range.getWwid()));
            }
            long physicalEnd;
            try {
                physicalEnd = Math.addExact(
                        range.getLunOffsetBytes(), range.getLengthBytes());
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(String.format(
                        "SharedBlock backend returned physical range overflow for LUN[%s]",
                        range.getWwid()));
            }
            if (physicalEnd > lun.getCapacityBytes()) {
                throw new IllegalArgumentException(String.format(
                        "SharedBlock backend returned range beyond capacity for LUN[%s]",
                        range.getWwid()));
            }
            referencedWwids.add(range.getWwid());
        }
        if (expectedOffset != layer.getLvSizeBytes()) {
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend ranges cover[%s] bytes but layer size is[%s] for snapshot[%s]",
                    expectedOffset, layer.getLvSizeBytes(),
                    target.getVolumeSnapshotUuid()));
        }
        validatePhysicalRanges(target, layer);
    }

    private static void validatePhysicalRanges(
            SharedBlockVolumeSnapshotLanFreeTarget target,
            SharedBlockVolumeSnapshotLanFreeLayer layer) {
        Map<String, List<SharedBlockLvRange>> rangesByWwid =
                layer.getRanges().stream().collect(Collectors.groupingBy(
                        SharedBlockLvRange::getWwid));
        for (Map.Entry<String, List<SharedBlockLvRange>> entry
                : rangesByWwid.entrySet()) {
            List<SharedBlockLvRange> physicalRanges =
                    new ArrayList<>(entry.getValue());
            physicalRanges.sort(Comparator.comparing(
                    SharedBlockLvRange::getLunOffsetBytes));
            long previousEnd = -1L;
            for (SharedBlockLvRange range : physicalRanges) {
                if (range.getLunOffsetBytes() < previousEnd) {
                    throw new IllegalArgumentException(String.format(
                            "SharedBlock backend returned overlapping physical ranges for LUN[%s] in snapshot[%s] layer[%s]",
                            entry.getKey(), target.getVolumeSnapshotUuid(),
                            layer.getLayerIndex()));
                }
                previousEnd = range.getLunOffsetBytes()
                        + range.getLengthBytes();
            }
        }
    }

    private static void validateLun(SharedBlockLunDescriptor lun) {
        if (lun == null || isBlank(lun.getWwid())
                || lun.getCapacityBytes() == null
                || lun.getCapacityBytes() <= 0) {
            throw new IllegalArgumentException(
                    "SharedBlock backend returned an invalid LUN descriptor");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean isValidSourceInstallPath(
            String primaryStorageUuid, String installPath) {
        String prefix = String.format(
                "sharedblock://%s/", primaryStorageUuid);
        if (!installPath.startsWith(prefix)) {
            return false;
        }
        String lvName = installPath.substring(prefix.length());
        return !isBlank(lvName) && !lvName.contains("/");
    }

    static final class Result {
        private final List<SharedBlockLunDescriptor> luns;
        private final List<SharedBlockVolumeSnapshotLanFreeLayout> layouts;

        Result(
                List<SharedBlockLunDescriptor> luns,
                List<SharedBlockVolumeSnapshotLanFreeLayout> layouts) {
            this.luns = luns;
            this.layouts = layouts;
        }

        List<SharedBlockLunDescriptor> getLuns() {
            return luns;
        }

        List<SharedBlockVolumeSnapshotLanFreeLayout> getLayouts() {
            return layouts;
        }
    }
}
