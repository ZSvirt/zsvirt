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

final class SharedBlockVolumeLanFreeLayoutAggregator {
    private SharedBlockVolumeLanFreeLayoutAggregator() {
    }

    static Result aggregate(
            List<SharedBlockVolumeLanFreeTarget> targets,
            List<GetSharedBlockVolumeLanFreeLayoutReply> replies) {
        if (targets == null || targets.isEmpty() || replies == null) {
            throw new IllegalArgumentException("Volume LAN-free targets and replies are required");
        }
        Map<String, SharedBlockVolumeLanFreeTarget> targetsByUuid = new HashMap<>();
        for (SharedBlockVolumeLanFreeTarget target : targets) {
            if (target == null || isBlank(target.getVolumeUuid())
                    || target.getRequestIndex() == null
                    || targetsByUuid.put(target.getVolumeUuid(), target) != null) {
                throw new IllegalArgumentException("duplicate or invalid Volume LAN-free target");
            }
        }

        Map<String, SharedBlockLunDescriptor> lunsByWwid = new LinkedHashMap<>();
        Map<String, SharedBlockVolumeLanFreeLayout> layoutsByUuid = new HashMap<>();
        for (GetSharedBlockVolumeLanFreeLayoutReply reply : replies) {
            if (reply == null || reply.getLuns() == null || reply.getLayouts() == null) {
                throw new IllegalArgumentException("SharedBlock backend returned null Volume LAN-free data");
            }
            for (SharedBlockLunDescriptor lun : reply.getLuns()) {
                if (lun == null || isBlank(lun.getWwid())
                        || lun.getCapacityBytes() == null || lun.getCapacityBytes() <= 0) {
                    throw new IllegalArgumentException("SharedBlock backend returned invalid LUN");
                }
                SharedBlockLunDescriptor existing = lunsByWwid.get(lun.getWwid());
                if (existing != null && !existing.getCapacityBytes().equals(lun.getCapacityBytes())) {
                    throw new IllegalArgumentException(
                            "SharedBlock backend returned conflicting capacities for LUN["
                                    + lun.getWwid() + "]");
                }
                lunsByWwid.putIfAbsent(lun.getWwid(), lun);
            }
            for (SharedBlockVolumeLanFreeLayout layout : reply.getLayouts()) {
                if (layout == null || isBlank(layout.getVolumeUuid())) {
                    throw new IllegalArgumentException("SharedBlock backend returned invalid Volume layout");
                }
                if (layoutsByUuid.put(layout.getVolumeUuid(), layout) != null) {
                    throw new IllegalArgumentException(
                            "SharedBlock backend returned duplicate Volume layout["
                                    + layout.getVolumeUuid() + "]");
                }
            }
        }

        if (!layoutsByUuid.keySet().equals(targetsByUuid.keySet())) {
            Set<String> missing = new LinkedHashSet<>(targetsByUuid.keySet());
            missing.removeAll(layoutsByUuid.keySet());
            Set<String> unexpected = new LinkedHashSet<>(layoutsByUuid.keySet());
            unexpected.removeAll(targetsByUuid.keySet());
            throw new IllegalArgumentException(String.format(
                    "SharedBlock backend returned mismatched Volume layouts, missing%s, unexpected%s",
                    missing, unexpected));
        }

        Set<String> referencedWwids = new HashSet<>();
        for (Map.Entry<String, SharedBlockVolumeLanFreeLayout> entry : layoutsByUuid.entrySet()) {
            validateLayout(targetsByUuid.get(entry.getKey()), entry.getValue(),
                    lunsByWwid, referencedWwids);
        }
        Set<String> unreferenced = new LinkedHashSet<>(lunsByWwid.keySet());
        unreferenced.removeAll(referencedWwids);
        if (!unreferenced.isEmpty()) {
            throw new IllegalArgumentException(
                    "SharedBlock backend returned unreferenced LUNs" + unreferenced);
        }

        List<SharedBlockVolumeLanFreeLayout> ordered = targets.stream()
                .sorted(Comparator.comparing(SharedBlockVolumeLanFreeTarget::getRequestIndex))
                .map(target -> layoutsByUuid.get(target.getVolumeUuid()))
                .collect(Collectors.toList());
        return new Result(new ArrayList<>(lunsByWwid.values()), ordered);
    }

    private static void validateLayout(
            SharedBlockVolumeLanFreeTarget target,
            SharedBlockVolumeLanFreeLayout layout,
            Map<String, SharedBlockLunDescriptor> luns,
            Set<String> referencedWwids) {
        if (!Objects.equals(target.getVolumeInstallPath(), layout.getVolumeInstallPath())) {
            throw new IllegalArgumentException(
                    "SharedBlock backend returned mismatched Volume install path["
                            + target.getVolumeUuid() + "]");
        }
        if ((!"qcow2".equals(layout.getFormat()) && !"raw".equals(layout.getFormat()))
                || layout.getVirtualSizeBytes() == null
                || layout.getVirtualSizeBytes() <= 0
                || layout.getLvSizeBytes() == null
                || layout.getLvSizeBytes() <= 0) {
            throw new IllegalArgumentException(
                    "SharedBlock backend returned invalid Volume layout metadata["
                            + target.getVolumeUuid() + "]");
        }
        if (layout.getRanges() == null || layout.getRanges().isEmpty()) {
            throw new IllegalArgumentException("SharedBlock backend returned no Volume ranges");
        }
        long expectedOffset = 0L;
        Map<String, List<SharedBlockLvRange>> physicalRanges = new HashMap<>();
        for (SharedBlockLvRange range : layout.getRanges()) {
            if (range == null || isBlank(range.getWwid())
                    || range.getLvOffsetBytes() == null
                    || range.getLunOffsetBytes() == null
                    || range.getLengthBytes() == null
                    || range.getLvOffsetBytes() != expectedOffset
                    || range.getLunOffsetBytes() < 0
                    || range.getLengthBytes() <= 0) {
                throw new IllegalArgumentException(
                        "SharedBlock backend returned invalid Volume range at LV offset["
                                + expectedOffset + "]");
            }
            SharedBlockLunDescriptor lun = luns.get(range.getWwid());
            if (lun == null) {
                throw new IllegalArgumentException(
                        "SharedBlock backend range references unknown LUN["
                                + range.getWwid() + "]");
            }
            try {
                expectedOffset = Math.addExact(expectedOffset, range.getLengthBytes());
                long physicalEnd = Math.addExact(
                        range.getLunOffsetBytes(), range.getLengthBytes());
                if (expectedOffset > layout.getLvSizeBytes()
                        || physicalEnd > lun.getCapacityBytes()) {
                    throw new IllegalArgumentException(
                            "SharedBlock backend Volume range exceeds LV or LUN capacity");
                }
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("SharedBlock backend Volume range overflow");
            }
            referencedWwids.add(range.getWwid());
            physicalRanges.computeIfAbsent(range.getWwid(), ignored -> new ArrayList<>())
                    .add(range);
        }
        if (expectedOffset != layout.getLvSizeBytes()) {
            throw new IllegalArgumentException(
                    "SharedBlock backend Volume ranges do not cover LV["
                            + target.getVolumeUuid() + "]");
        }
        for (Map.Entry<String, List<SharedBlockLvRange>> entry : physicalRanges.entrySet()) {
            List<SharedBlockLvRange> sorted = entry.getValue();
            sorted.sort(Comparator.comparing(SharedBlockLvRange::getLunOffsetBytes));
            long previousEnd = -1L;
            for (SharedBlockLvRange range : sorted) {
                if (range.getLunOffsetBytes() < previousEnd) {
                    throw new IllegalArgumentException(
                            "SharedBlock backend returned overlapping physical Volume ranges for LUN["
                                    + entry.getKey() + "]");
                }
                previousEnd = range.getLunOffsetBytes() + range.getLengthBytes();
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static final class Result {
        private final List<SharedBlockLunDescriptor> luns;
        private final List<SharedBlockVolumeLanFreeLayout> layouts;

        Result(List<SharedBlockLunDescriptor> luns,
               List<SharedBlockVolumeLanFreeLayout> layouts) {
            this.luns = luns;
            this.layouts = layouts;
        }

        List<SharedBlockLunDescriptor> getLuns() {
            return luns;
        }

        List<SharedBlockVolumeLanFreeLayout> getLayouts() {
            return layouts;
        }
    }
}
