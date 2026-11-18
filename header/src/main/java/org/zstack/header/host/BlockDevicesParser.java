package org.zstack.header.host;

import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.*;

public class BlockDevicesParser {
    public static class BlockDevice {
        private String name;
        private String type;
        private long size;
        @SerializedName(value = "phy-sec")
        private long physicalSector;
        @SerializedName(value = "log-sec")
        private long logicalSector;
        @SerializedName(value = "mountpoint")
        private String mountPoint;
        private List<BlockDevice> children;
        private String partitionTable;
        private String fstype;
        private String serial;
        private String model;

        BlockDevice() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getPhysicalSector() {
            return physicalSector;
        }

        public void setPhysicalSector(long physicalSector) {
            this.physicalSector = physicalSector;
        }

        public long getLogicalSector() {
            return logicalSector;
        }

        public void setLogicalSector(long logicalSector) {
            this.logicalSector = logicalSector;
        }

        public String getMountPoint() {
            return mountPoint;
        }

        public void setMountPoint(String mountPoint) {
            this.mountPoint = mountPoint;
        }

        public List<BlockDevice> getChildren() {
            return children;
        }

        public void setChildren(List<BlockDevice> children) {
            this.children = children;
        }

        public String getPartitionTable() {
            return partitionTable;
        }

        public void setPartitionTable(String partitionTable) {
            this.partitionTable = partitionTable;
        }

        public String getFstype() {
            return fstype;
        }

        public void setFstype(String fstype) {
            this.fstype = fstype;
        }

        public String getSerial() {
            return serial;
        }

        public void setSerial(String serial) {
            this.serial = serial;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static  String blockDevicesExample = "{\n" +
            "   \"blockdevices\": [\n" +
            "      {\"name\": \"/dev/sr0\", \"type\": \"rom\", \"size\": \"8019515392\", \"phy-sec\": \"2048\", \"log-sec\": \"2048\", \"mountpoint\": null},\n" +
            "      {\"name\": \"/dev/vda\", \"type\": \"disk\", \"size\": \"429496729600\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": null,\n" +
            "         \"children\": [\n" +
            "            {\"name\": \"/dev/vda1\", \"type\": \"part\", \"size\": \"1073741824\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": \"/boot\"},\n" +
            "            {\"name\": \"/dev/vda2\", \"type\": \"part\", \"size\": \"428421939200\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": null,\n" +
            "               \"children\": [\n" +
            "                  {\"name\": \"/dev/mapper/zstack-root\", \"type\": \"lvm\", \"size\": \"419950493696\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": \"/\"},\n" +
            "                  {\"name\": \"/dev/mapper/zstack-swap\", \"type\": \"lvm\", \"size\": \"8468299776\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": \"[SWAP]\"}\n" +
            "               ]\n" +
            "            }\n" +
            "         ]\n" +
            "      },\n" +
            "      {\"name\": \"/dev/vdb\", \"type\": \"disk\", \"size\": \"322122547200\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": null},\n" +
            "      {\"name\": \"/dev/vdc\", \"type\": \"disk\", \"size\": \"429496729600\", \"phy-sec\": \"512\", \"log-sec\": \"512\", \"mountpoint\": null}\n" +
            "   ]\n" +
            "}\n" +
            "===\r\n" +
            "/dev/vda:\r\n" +
            "/dev/vdb:msdos\r\n" +
            "/dev/vdc:unknown\r\n";

    public static List<BlockDevice> parse(String blockDevicesStdout) {
        List<BlockDevice> allBlockDevices = new ArrayList<>();

        List<String> splits = Arrays.asList(blockDevicesStdout.split("===\\r\\n"));
        LinkedHashMap blockDevicesMap = JSONObjectUtil.toObject(splits.get(0), LinkedHashMap.class);
        List<LinkedTreeMap> blockDeviceList = (List<LinkedTreeMap>) blockDevicesMap.get("blockdevices");
        blockDeviceList.forEach(blockDevice -> allBlockDevices.add(JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(blockDevice), BlockDevice.class)));

        Map<String, String> blockDevicePartitionTable = new HashMap<>();
        Arrays.asList(splits.get(1).split("\\r\\n")).forEach(it -> {
            List<String> blockDeviceAndPartitionTable = Arrays.asList(it.split(":"));
            if (blockDeviceAndPartitionTable.size() != 2) {
                return;
            }
            blockDevicePartitionTable.put(blockDeviceAndPartitionTable.get(0), blockDeviceAndPartitionTable.get(1));
        });
        allBlockDevices.forEach(blockDevice -> blockDevice.setPartitionTable(blockDevicePartitionTable.getOrDefault(blockDevice.getName(), null)));

        return allBlockDevices;
    }

    public static void collectBlockDeviceIdentifiers(String blockDevicesStdout, Map<String, String> wwids,
                                                    Map<String, String> wwns) {
        String[] sections = blockDevicesStdout.split("===\\r\\n", -1);
        Map<String, Object> snapshot = JSONObjectUtil.toObject(sections[0], LinkedHashMap.class);
        List<Map<String, Object>> devices = (List<Map<String, Object>>) snapshot.get("blockdevices");
        if (sections.length > 2) {
            for (String line : sections[2].split("\\r?\\n")) {
                String[] entry = line.split(":", 2);
                if (entry.length == 2) {
                    wwids.put(entry[0], entry[1].trim());
                }
            }
            recordMissingLunIdentities(devices, wwids);
        }
        collectWwns(devices, wwns);
    }

    private static void recordMissingLunIdentities(List<Map<String, Object>> devices, Map<String, String> wwids) {
        for (Map<String, Object> device : devices) {
            String name = (String) device.get("name");
            boolean lun = "mpath".equals(device.get("type")) || "disk".equals(device.get("type")) &&
                    (name.startsWith("/dev/nvme") || "fc".equals(device.get("tran")) || "iscsi".equals(device.get("tran")));
            if (lun) {
                wwids.putIfAbsent(name, "");
            }
            if (device.get("children") != null) {
                recordMissingLunIdentities((List<Map<String, Object>>) device.get("children"), wwids);
            }
        }
    }

    private static void collectWwns(List<Map<String, Object>> devices, Map<String, String> wwns) {
        for (Map<String, Object> device : devices) {
            String wwn = (String) device.get("wwn");
            if (wwn != null && !wwn.trim().isEmpty()) {
                wwns.put((String) device.get("name"), wwn.trim());
            }
            if (device.get("children") != null) {
                collectWwns((List<Map<String, Object>>) device.get("children"), wwns);
            }
        }
    }

    public static String normalizeIdentifier(String identifier) {
        return identifier == null ? null : identifier.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeWwn(String wwn) {
        String normalized = normalizeIdentifier(wwn);
        return normalized != null && normalized.startsWith("0x") ? normalized.substring(2) : normalized;
    }

    public static List<BlockDevice> parse(List<String> blockDevices) {
        List<BlockDevice> allBlockDevices = new ArrayList<>();
        blockDevices.forEach(blockDevice -> {
            LinkedHashMap blockDeviceMap = JSONObjectUtil.toObject(blockDevice, LinkedHashMap.class);
            allBlockDevices.add(JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(blockDeviceMap), BlockDevice.class));
        });
        return allBlockDevices;
    }

    public static String getBlockDevicesCommand() {
        return getBlockDevicesCommand(false);
    }

    // Physical-machine disk queries use SSH without requiring a registered Host or kvmagent.
    public static String getBlockDevicesCommandWithIdentifiers() {
        return getBlockDevicesCommand(true);
    }

    private static String getBlockDevicesCommand(boolean collectIdentifiers) {
        String blockDevicesCommand = "lsblk -p -b -o NAME,TYPE,SIZE,PHY-SEC,LOG-SEC,MOUNTPOINT" +
                (collectIdentifiers ? ",WWN,TRAN" : "") + " -J";
        String partitionTableInfoCommand = "for disk in $(lsblk -d -p -n -o NAME,TYPE | awk '$2!~/loop|rom/ {print $1}'); do echo -n \"$disk:\"; " +
                "parted -s $disk print 2>/dev/null | awk '/Partition Table/ {print $3} END{print \"\"}'; done";
        String command = String.format("%s ; echo === ; %s", blockDevicesCommand, partitionTableInfoCommand);
        if (!collectIdentifiers) {
            return command;
        }
        String identities = "timeout -k 1 60 lsblk -p -n -r -o NAME,TYPE,TRAN | while read -r disk type transport; do " +
                "dev=$(basename \"$disk\"); required=; wwid=; result=0; " +
                "case \"$type:$dev:$transport\" in mpath:*|disk:nvme*|disk:*:fc|disk:*:iscsi) required=1 ;; esac; " +
                "resolved=$(timeout -k 1 60 readlink -f \"$disk\") || result=$?; " +
                "dev=$(basename \"$resolved\"); " +
                "if [ \"$result\" -eq 0 ]; then case \"$type:$dev\" in " +
                "disk:nvme*) wwid=$(timeout -k 1 60 cat \"/sys/class/block/$dev/wwid\" 2>/dev/null); result=$? ;; " +
                "mpath:*) wwid=$(timeout -k 1 60 sed -n 's/^mpath-//p' \"/sys/class/block/$dev/dm/uuid\" 2>/dev/null); result=$? ;; " +
                "disk:*) if [ -x /usr/lib/udev/scsi_id ]; then wwid=$(timeout -k 1 60 /usr/lib/udev/scsi_id -g -u -d \"$disk\" 2>/dev/null); result=$?; " +
                "else scsi=$(timeout -k 1 60 lsscsi -i 2>/dev/null); result=$?; " +
                "wwid=$(printf '%s\\n' \"$scsi\" | awk -v disk=\"$disk\" '$(NF-1)==disk {print $NF}'); fi ;; esac; fi; " +
                "if [ \"$result\" -ne 0 ] || [ \"$wwid\" = - ]; then wwid=; fi; " +
                "if [ -n \"$wwid\" ] || [ -n \"$required\" ]; then printf '%s:%s\\n' \"$disk\" \"$wwid\"; fi; done";
        return command + " ; echo === ; " + identities;
    }
}
