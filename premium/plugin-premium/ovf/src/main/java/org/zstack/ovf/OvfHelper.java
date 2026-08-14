package org.zstack.ovf;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.yaml.snakeyaml.Yaml;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.config.schema.GuestOsCategory;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudOperationError;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageArchitecture;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageBootMode;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.ovf.datatype.*;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.ovf.datatype.OvfErrors.FAIL_TO_PARSE_IMAGE_INFO;
import static org.zstack.ovf.datatype.OvfErrors.INVALID_IMAGE_INFO;

/**
 * Created by Qi Le on 2022/3/3
 */
public class OvfHelper {
    private static final CLogger logger = Utils.getLogger(OvfHelper.class);

    private static final String VMW_OS_TYPE_MAPPER_PATH = "ovf/vmwOsTypeMapper.yaml";
    private static final String EXPORT_OS_TYPE_MAPPER_PATH = "ovf/ExporterOsTypeMapper.yaml";
    private static final String INSTANCE_ID = "InstanceID";
    private static final String ELEMENT_NAME = "ElementName";

    private static final String XML_NAMESPACE_URI = "http://schemas.dmtf.org/ovf/envelope/1";

    public static OvfInfo parseOvf(Document document) {
        OvfInfo res = new OvfInfo();

        Map<String, String> refMap = new HashMap<>();
        Element root = document.getRootElement();
        Element section = root.element("References");
        if (section != null) {
            for (Iterator<Element> it = section.elementIterator("File"); it.hasNext(); ) {
                Element e = it.next();
                String id = e.attributeValue("id");
                if (id != null) {
                    refMap.put(id, e.attributeValue("href"));
                }
            }
        }

        section = root.element("DiskSection");
        List<OvfDiskInfo> disks = new ArrayList<>();
        res.setDisks(disks);
        if (section != null) {
            for (Iterator<Element> it = section.elementIterator("Disk"); it.hasNext(); ) {
                Element e = it.next();
                String id = e.attributeValue("diskId");
                OvfDiskInfo diskInfo;
                if (id == null) {
                    continue;
                } else {
                    diskInfo = new OvfDiskInfo();
                    diskInfo.setDiskId(id);
                    diskInfo.setIndex(disks.size());
                    disks.add(diskInfo);
                }

                String ref = e.attributeValue("fileRef");
                if (ref != null) {
                    String fileName = refMap.get(ref);
                    if (fileName == null) {
                        throw new OperationFailureException(
                                Platform.operr("File reference not fount for disk %s", id));
                    }
                    diskInfo.setFileRef(ref);
                    diskInfo.setFileName(fileName);
                }

                SizeUnit unit = getSizeUnit(e.attributeValue("capacityAllocationUnits"));

                String capacity = e.attributeValue("capacity");
                if (capacity != null) {
                    try {
                        diskInfo.setCapacity(unit.toByte(Long.parseLong(capacity)));
                    } catch (NumberFormatException ex) {
                        throw new OperationFailureException(Platform.operr("Illegal disk capacity: %s", capacity));
                    }
                }

                String pSize = e.attributeValue("populatedSize");
                if (pSize != null) {
                    try {
                        diskInfo.setPopulatedSize(Long.parseLong(pSize));
                    } catch (NumberFormatException ex) {
                        throw new OperationFailureException(
                                Platform.operr("Illegal disk populated size: %s", pSize));
                    }
                }

                String format = e.attributeValue("format");
                if (format != null) {
                    if (StringUtils.contains(format, "vmdk")) {
                        diskInfo.setFormat("vmdk");
                    } else if (StringUtils.contains(format, "qcow2")) {
                        diskInfo.setFormat("qcow2");
                    }
                }
            }
        }


        section = root.element("NetworkSection");
        List<OvfNetworkInfo> networks = new ArrayList<>();
        res.setNetworks(networks);
        if (section != null) {
            for (Iterator<Element> it = section.elementIterator("Network"); it.hasNext(); ) {
                Element e = it.next();
                String name = e.attributeValue("name");
                OvfNetworkInfo network;
                if (name != null) {
                    network = new OvfNetworkInfo();
                    network.setName(name);
                    networks.add(network);
                }
            }
        }

        Element virtualSystem = root.element("VirtualSystem");
        parseVirtualSystem(virtualSystem, res);
        createPreAnalysisInfo(res);

        return res;
    }

    private static void parseVirtualSystem(Element vs, OvfInfo res) {
        if (vs == null) {
            return;
        }
        String id = vs.attributeValue("id");
        String name = vs.elementText("Name");
        if (name != null) {
            res.setVmName(name);
        } else if (id != null) {
            res.setVmName(id);
        } else {
            res.setVmName("");
        }

        Element section = vs.element("OperatingSystemSection");
        OvfOSInfo osInfo = new OvfOSInfo();
        res.setOs(osInfo);
        if (section != null) {
            String osId = section.attributeValue("id");
            if (osId != null) {
                try {
                    osInfo.setId(Integer.parseInt(osId));
                } catch (NumberFormatException ignored) {
                    logger.warn(String.format("Invalid os id: %s", id));
                }
            }
            String version = section.attributeValue("version");
            if (version != null) {
                osInfo.setVersion(version);
            }
            String osType = section.attributeValue("osType");
            if (osType != null) {
                Map<String, Map<String, String>> map = getOsTypeMap(EXPORT_OS_TYPE_MAPPER_PATH);

                // find key of value maps where vmwOsName's value matches osType in values
                String guestOsType = map.entrySet().stream()
                        .filter(entry -> osType.equals(entry.getValue().get("vmwOsName")))
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(null);

                osInfo.setOsType(guestOsType != null ? guestOsType : ImagePlatform.Other.toString());
            }
            String osDescription = section.elementText("Description");
            if (osDescription != null) {
                osInfo.setDescription(osDescription);
            }
        }

        Element hardwareSection = vs.element("VirtualHardwareSection");
        parseVirtualHardware(hardwareSection, res);
    }

    private static void parseVirtualHardware(Element hw, OvfInfo res) {
        if (hw == null) {
            return;
        }
        Element systemSection = hw.element("System");
        OvfSystemInfo systemInfo = new OvfSystemInfo();
        res.setSystemInfo(systemInfo);
        if (systemSection != null) {
            String type = systemSection.elementText("VirtualSystemType");
            if (type != null) {
                systemInfo.setVirtualSystemType(type);
            } else {
                systemInfo.setVirtualSystemType("");
            }
        }

        for (Iterator<Element> it = hw.elementIterator("Config"); it.hasNext(); ) {
            Element e = it.next();
            if ("firmware".equals(e.attributeValue("key"))) {
                systemInfo.setFirmwareType(e.attributeValue("value"));
            }
        }

        List<Element> nics = new ArrayList<>();
        List<Element> cds = new ArrayList<>();
        List<Element> volumes = new ArrayList<>();
        Map<String, String> controllerMap = new HashMap<>();
        for (Iterator<Element> it = hw.elementIterator("Item"); it.hasNext(); ) {
            Element e = it.next();
            String resourceType = e.elementText("ResourceType");
            switch (resourceType) {
                case "3": //Processor
                    parseCpu(e, res);
                    break;
                case "4": //Memory
                    parseMemory(e, res);
                    break;
                case "10"://Ethernet Adapter
                    nics.add(e);
                    break;
                case "15"://CD Driver
                    cds.add(e);
                    break;
                case "17"://Volume
                    volumes.add(e);
                    break;
                case "5": //IDE Controller
                    String id = e.elementText(INSTANCE_ID);
                    controllerMap.put(id, "IDE");
                    break;
                case "6": //SCSI Controller
                    id = e.elementText(INSTANCE_ID);
                    controllerMap.put(id, "SCSI");
                    break;
                case "20"://Other Storage (SATA)
                    id = e.elementText(INSTANCE_ID);
                    controllerMap.put(id, "SATA");
                    break;
                default:
            }
        }

        parseNic(nics, res);
        parseCD(cds, controllerMap, res);
        parseVolume(volumes, controllerMap, res);
    }

    private static void parseVolume(List<Element> volumes, Map<String, String> controllerMap, OvfInfo res) {
        List<OvfVolumeInfo> volumeInfos = new ArrayList<>();
        res.setVolumes(volumeInfos);
        for (Element e : volumes) {
            OvfVolumeInfo volumeInfo = new OvfVolumeInfo();
            String parent = e.elementText("Parent");
            if (parent == null) {
                // Volume not connect to Virtual System
                continue;
            }
            String type = controllerMap.get(parent);
            if (type == null) {
                throw new OperationFailureException(Platform.operr("Volume controller not found."));
            }
            volumeInfo.setDriverType(type);
            volumeInfos.add(volumeInfo);

            String name = e.elementText(ELEMENT_NAME);
            if (name != null) {
                volumeInfo.setName(name);
            } else {
                volumeInfo.setName("");
            }

            String resource = e.elementText("HostResource");
            String diskId = StringUtils.substringAfterLast(resource, "/");
            if (diskId != null) {
                volumeInfo.setDiskId(diskId);
            }
        }
    }

    private static void parseCD(List<Element> cds, Map<String, String> controllerMap, OvfInfo res) {
        List<OvfCdDriverInfo> cdDriverInfos = new ArrayList<>();
        res.setCdDrivers(cdDriverInfos);
        for (Element e : cds) {
            OvfCdDriverInfo cdDriver = new OvfCdDriverInfo();
            String parent = e.elementText("Parent");
            if (parent == null) {
                // CD Driver not connect to Virtual System
                continue;
            }
            String type = controllerMap.get(parent);
            if (type == null) {
                throw new OperationFailureException(Platform.operr("CD Driver controller not found."));
            }
            cdDriver.setDriverType(type);
            cdDriverInfos.add(cdDriver);

            String subType = e.elementText("ResourceSubType");
            if (subType != null) {
                cdDriver.setSubType(subType);
            }

            String name = e.elementText(ELEMENT_NAME);
            if (name != null) {
                cdDriver.setName(name);
            }

            String autoAloc = e.elementText("AutomaticAllocation");
            if (autoAloc != null) {
                cdDriver.setAutoAllocation(Boolean.parseBoolean(autoAloc));
            }
        }
    }

    private static void parseNic(List<Element> nics, OvfInfo res) {
        List<OvfEthernetAdapterInfo> nicInfos = new ArrayList<>();
        res.setNics(nicInfos);
        for (Element e : nics) {
            OvfEthernetAdapterInfo nicInfo = new OvfEthernetAdapterInfo();
            nicInfos.add(nicInfo);
            String model = e.elementText("ResourceSubType");
            if (model != null) {
                nicInfo.setNicModel(model);
            }

            String name = e.elementText(ELEMENT_NAME);
            if (name != null) {
                nicInfo.setNicName(name);
            }

            String autoAlc = e.elementText("AutomaticAllocation");
            if (autoAlc != null) {
                nicInfo.setAutoAllocation(Boolean.parseBoolean(autoAlc));
            }

            String networkName = e.elementText("Connection");
            if (networkName != null) {
                List<OvfNetworkInfo> networks = res.getNetworks();
                if (networks.stream().noneMatch(n -> networkName.equals(n.getName()))) {
                    throw new OperationFailureException(
                            Platform.operr("Ethernet Adapter: %s do not connect to a network.", name));
                }
                nicInfo.setNetworkName(networkName);
            }
        }
    }

    private static void parseMemory(Element memory, OvfInfo res) {
        OvfMemoryInfo memoryInfo = new OvfMemoryInfo();
        res.setMemory(memoryInfo);
        String id = memory.elementText(INSTANCE_ID);
        if (id == null) {
            throw new OperationFailureException(Platform.operr("Memory 'InstanceID' not found"));
        }
        memoryInfo.setInstanceId(id);

        SizeUnit unit = getSizeUnit(memory.elementText("AllocationUnits"));
        String quantity = memory.elementText("VirtualQuantity");
        if (quantity == null) {
            throw new OperationFailureException(Platform.operr("Memory 'VirtualQuantity' not found"));
        }
        try {
            memoryInfo.setQuantity(unit.toByte(Long.parseLong(quantity)));
        } catch (NumberFormatException e) {
            throw new OperationFailureException(Platform.operr("Illegal Memory 'VirtualQuantity' value: %s", quantity));
        }
    }

    private static void parseCpu(Element cpu, OvfInfo res) {
        OvfCpuInfo cpuInfo = new OvfCpuInfo();
        res.setCpu(cpuInfo);
        String id = cpu.elementText(INSTANCE_ID);
        if (id == null) {
            throw new OperationFailureException(Platform.operr("CPU 'InstanceID' not found"));
        }
        cpuInfo.setInstanceId(id);

        String quantity = cpu.elementText("VirtualQuantity");
        if (quantity == null) {
            throw new OperationFailureException(Platform.operr("CPU 'VirtualQuantity' not found"));
        }
        try {
            cpuInfo.setQuantity(Integer.parseInt(quantity));
        } catch (NumberFormatException e) {
            throw new OperationFailureException(Platform.operr("Illegal CPU 'VirtualQuantity' value: %s", quantity));
        }

        String cps = cpu.elementText("CoresPerSocket");
        if (cps == null) {
            cpuInfo.setCoresPerSocket(cpuInfo.getQuantity());
        } else {
            try {
                cpuInfo.setCoresPerSocket(Integer.parseInt(cps));
            } catch (NumberFormatException e) {
                throw new OperationFailureException(Platform.operr("Illegal CPU 'CoresPerSocket' value: %s", cps));
            }
        }
    }

    private static void createPreAnalysisInfo(OvfInfo res) {
        OvfPreAnalysisInfo info = new OvfPreAnalysisInfo();
        res.setPreAnalysisInfo(info);

        info.setInferredPlatform(getImagePlatformFromOVF(res));
        info.setInferredArchitecture(getImageArchitectureFromOVF(res));

        if (!res.getDisks().isEmpty()) {
            final OvfDiskInfo firstDisk = res.getDisks().get(0);
            info.setInferredRootDiskSize(firstDisk.getCapacity());
        }
    }

    private static SizeUnit getSizeUnit(String unit) {
        if (unit == null || StringUtils.equalsIgnoreCase(SizeUnit.BYTE.getFullName(), unit)) {
            return SizeUnit.BYTE;
        }
        switch (StringUtils.substring(unit, -4)) {
            case "2^10":
                return SizeUnit.KILOBYTE;
            case "2^20":
                return SizeUnit.MEGABYTE;
            case "2^30":
                return SizeUnit.GIGABYTE;
            case "2^40":
                return SizeUnit.TERABYTE;
            case "2^50":
                return SizeUnit.PETABYTE;
            default:
                return SizeUnit.BYTE;
        }
    }

    public static List<CreateVmFromOvfImageParam> parseCreateVmFromOvfParams(String jsonText) throws OperationFailureException {
        List<CreateVmFromOvfImageParam> list;
        try {
            list = JSONObjectUtil.toList(jsonText, new TypeToken<List<CreateVmFromOvfImageParam>>() {}.getType());
        } catch (JsonSyntaxException e) {
            throw new OperationFailureException(err(FAIL_TO_PARSE_IMAGE_INFO,
                    "failed to parse image info, because %s", e.getMessage()
            ));
        }

        for (CreateVmFromOvfImageParam param : list) {
            ErrorCode errorCode = param.validate();
            if (errorCode != null) {
                throw err(INVALID_IMAGE_INFO, "invalid image info[fileName=%s]", param.getFileName())
                        .withCause(errorCode)
                        .toException();
            }
        }

        return list;
    }

    public static ImagePlatform getImagePlatformFromOVF(OvfInfo ovf) {
        // OVF file allows OS to be empty
        if (ovf.getOs() == null) {
            logger.debug("empty ovf OS information, regard it as other platform type");
            return ImagePlatform.Other;
        }

        if (ovf.getOs().getId() != null) PARSE_OS_ID: {
            Map<String, Map<String, String>> map = getOsTypeMap(VMW_OS_TYPE_MAPPER_PATH);
            Map<String, String> param = map.get(ovf.getOs().getId().toString());
            if (param == null) {
                break PARSE_OS_ID;
            }
            String platform = param.get("platform");
            switch (platform) {
                case "Linux":
                    return ImagePlatform.Linux;
                case "Windows":
                    return ImagePlatform.Windows;
                case "FreeBSD": case "Other":
                    return ImagePlatform.Other;
            }
        }

        if (ovf.getOs().getOsType() != null) {
            String os = ovf.getOs().getOsType().toLowerCase();
            if (os.contains("window")) {
                return ImagePlatform.Windows;
            }
            if (os.contains("linux") || os.contains("centos") || os.contains("debian") || os.contains("ubuntu")) {
                return ImagePlatform.Linux;
            }
        }

        logger.debug(String.format("unknown OS information[id=%d, osType=%s], regard it as other platform type",
                ovf.getOs().getId(), ovf.getOs().getOsType()));
        return ImagePlatform.Other;
    }

    @Nullable
    public static ImageArchitecture getImageArchitectureFromOVF(OvfInfo ovf) {
        // OVF file allows OS to be empty
        if (ovf.getOs() == null) {
            logger.debug("empty ovf OS information, regard it as unknown architecture type");
            return null;
        }

        if (ovf.getOs().getId() != null) PARSE_OS_ID: {
            Map<String, Map<String, String>> map = getOsTypeMap(VMW_OS_TYPE_MAPPER_PATH);
            Map<String, String> param = map.get(ovf.getOs().getId().toString());
            if (param == null) {
                break PARSE_OS_ID;
            }
            String arch = param.get("arch");
            switch (arch) {
                case "x86": case "x64": case "x86_64":
                    return ImageArchitecture.x86_64;
                default:
                    return null;
            }
        }

        if (ovf.getOs().getOsType() != null) {
            String os = ovf.getOs().getOsType().toLowerCase();
            if (os.contains("arm") || os.contains("aarch")) {
                return ImageArchitecture.aarch64;
            }
            if (os.contains("64")) {
                return ImageArchitecture.x86_64;
            }
        }

        logger.debug(String.format("unknown OS information[id=%d, osType=%s], regard it as unknown architecture type",
                ovf.getOs().getId(), ovf.getOs().getOsType()));
        return null;
    }

    private static Map<String, Map<String, String>> getOsTypeMap(String filePath) {
        File mapperFile = PathUtil.findFileOnClassPath(filePath);
        if (mapperFile == null) {
            throw new CloudRuntimeException(
                    String.format("failed to find mapper file: %s", filePath));
        }
        Map<String, Map<String, String>> map;
        try {
            map = new Yaml().load(Files.newInputStream(mapperFile.toPath()));
        } catch (IOException e) {
            throw new CloudRuntimeException(
                    String.format("failed to load mapper file: %s", filePath), e);
        }
        return map;
    }

    public static String getGuestOsTypeFromOVF(OvfInfo ovf) {
        // OVF file format allows OS to be empty
        if (ovf.getOs() == null || ovf.getOs().getOsType() == null) {
            logger.debug("empty ovf OS information, regarded it as other OS type");
            return GuestOsCategory.getDefaultOsRelease();
        }
        return ovf.getOs().getOsType();
    }

    public static String generateOvfString(String vmUuid) {
        if (vmUuid == null) {
            return null;
        }
        VmInstanceVO vmvo = Q.New(VmInstanceVO.class).eq(VmInstanceAO_.uuid, vmUuid).find();
        if (vmvo == null) {
            return null;
        }
        VmInstanceInventory vm = VmInstanceInventory.valueOf(vmvo);
        return generateOvfString(vm);
    }

    public static String generateOvfString(VmInstanceInventory vm) {
        Document ovf = generateOvfFromVm(vm);
        return writeOvfString(ovf);
    }

    public static String writeOvfString(Document ovf) {
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        format.setNewLineAfterDeclaration(false);
        StringBuilderWriter resWriter = new StringBuilderWriter();
        XMLWriter writer = new XMLWriter(resWriter, format);
        try {
            writer.write(ovf);
        } catch (IOException e) {
            throw new CloudRuntimeException("write ovf file failed.", e);
        }
        return resWriter.toString();
    }

    public static Document generateOvfFromVm(VmInstanceInventory vm) {
        Document ovf = DocumentHelper.createDocument();
        Element envelope = getOvfEnvelope(ovf);
        List<VolumeInventory> volumes = vm.getAllDiskVolumes().stream()
                .sorted(Comparator.comparing(VolumeInventory::getDeviceId))
                .filter(vol -> !vol.isShareable())
                .collect(Collectors.toList());
        List<OvfDiskInfo> diskInfos = setReferences(volumes.size(), vm.getUuid(), envelope);
        setDiskSection(volumes, envelope, diskInfos);

        List<VmNicInventory> nics = vm.getVmNics();
        List<OvfEthernetAdapterInfo> networkInfos = setNetworkSection(nics, envelope);
        setVirtualSystem(vm, envelope, diskInfos, networkInfos);
        return ovf;
    }

    private static void setVirtualSystem(VmInstanceInventory vm, Element envelope, List<OvfDiskInfo> diskInfos, List<OvfEthernetAdapterInfo> networkInfos) {
        Element virtualSystem = envelope.addElement("VirtualSystem", XML_NAMESPACE_URI)
                .addAttribute("ovf:id", vm.getName());
        virtualSystem.addElement("Info").addText("A virtual machine");
        virtualSystem.addElement("Name").addText(vm.getName());
        setOperatingSystemSection(vm, virtualSystem);
        setVirtualHardwareSection(vm, virtualSystem, diskInfos, networkInfos);
    }

    private static void setVirtualHardwareSection(VmInstanceInventory vm, Element parent, List<OvfDiskInfo> diskInfos, List<OvfEthernetAdapterInfo> networkInfos) {
        Element virtualHardware = parent.addElement("VirtualHardwareSection");
        virtualHardware.addElement("Info").addText("Virtual hardware requirements");
        setSystemSection(virtualHardware);
        setCpuItem(vm, virtualHardware);
        setMemoryItem(vm, virtualHardware);
        int instanceId = setDiskItem(diskInfos, virtualHardware);
        setNicItem(networkInfos, instanceId, virtualHardware);
        setOtherConfig(vm, virtualHardware);
    }

    private static void setOtherConfig(VmInstanceInventory vm, Element parent) {
        String bootMode = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.BOOT_MODE_TOKEN);
        String firmware = "bios";
        if (ImageBootMode.UEFI.toString().equals(bootMode) || ImageBootMode.UEFI_WITH_CSM.toString().equals(bootMode)) {
            firmware = "efi";
        }
        parent.addElement("vmw:Config")
                .addAttribute("ovf:required", "false")
                .addAttribute("vmw:key", "firmware")
                .addAttribute("vmw:value", firmware);
    }

    private static int setNicItem(List<OvfEthernetAdapterInfo> networkInfos, int instanceId, Element parent) {
        for (int i = 0; i < networkInfos.size(); i++) {
            OvfEthernetAdapterInfo nic = networkInfos.get(i);
            Element network = parent.addElement("Item");
            network.addElement("rasd:AddressOnParent").addText(Integer.toString(i));
            network.addElement("rasd:AutomaticAllocation").addText("true");
            network.addElement("rasd:Connection").addText(nic.getNetworkName());
            network.addElement("rasd:ElementName").addText(String.format("Network adapter %d", i + 1));
            network.addElement("rasd:InstanceID").addText(Integer.toString(instanceId++));
            network.addElement("rasd:ResourceSubType").addText(nic.getNicModel());
            network.addElement("rasd:ResourceType").addText(Integer.toString(10));
            network.addElement("vmw:Config")
                    .addAttribute("ovf:required", "false")
                    .addAttribute("vmw:key", "wakeOnLanEnabled")
                    .addAttribute("vmw:value", "false");
        }
        return instanceId;
    }

    private static int setDiskItem(List<OvfDiskInfo> diskInfos, Element parent) {
        int diskNum = diskInfos.size();
        int instanceId = 3;
        instanceId += setDiskController(diskNum, parent);
        for (int i = 0, j = 0; i < diskInfos.size(); i++, j++) {
            OvfDiskInfo diskInfo = diskInfos.get(i);
            if (i == 3 || i == 6) {
                j = 0;
            }

            Element disk = parent.addElement("Item");
            disk.addElement("rasd:AddressOnParent").addText(Integer.toString(j));
            disk.addElement("rasd:ElementName").addText(String.format("Hard Disk %d", i + 1));
            disk.addElement("rasd:HostResource").addText(String.format("ovf:/disk/%s", diskInfo.getDiskId()));
            disk.addElement("rasd:InstanceID").addText(Integer.toString(instanceId++));
            Element parentId = disk.addElement("rasd:Parent");
            disk.addElement("rasd:ResourceType").addText(Integer.toString(17));

            if (i < 3) {
                parentId.addText("3");
            } else if (i < 6) {
                parentId.addText("4");
            } else {
                parentId.addText("5");
            }
        }
        return instanceId;
    }

    private static int setDiskController(int diskNum, Element parent) {
        setIdeController(0, parent);
        int n = 1;
        if (diskNum > 3) {
            setIdeController(1, parent);
            ++n;
        }
        if (diskNum > 6) {
            setScsiController(parent);
            ++n;
        }
        return n;
    }

    private static void setScsiController(Element parent) {
        Element controller = parent.addElement("Item");
        controller.addElement("rasd:Address").addText(Integer.toString(0));
        controller.addElement("rasd:Description").addText("SCSI Controller");
        controller.addElement("rasd:ElementName").addText("SCSI controller 0");
        controller.addElement("rasd:InstanceID").addText(Integer.toString(5));
        controller.addElement("rasd:ResourceType").addText(Integer.toString(6));
        controller.addElement("vmw:Config")
                .addAttribute("ovf:required", "false")
                .addAttribute("vmw:key", "slotInfo.pciSlotNumber")
                .addAttribute("vmw:value", Integer.toString(16));
    }

    private static void setIdeController(int index, Element parent) {

        Element controller = parent.addElement("Item");
        controller.addElement("rasd:Address").addText(Integer.toString(index));
        controller.addElement("rasd:Description").addText("IDE Controller");
        controller.addElement("rasd:ElementName").addText(String.format("VirtualIDEController %d", index));
        controller.addElement("rasd:InstanceID").addText(Integer.toString(3 + index));
        controller.addElement("rasd:ResourceType").addText(Integer.toString(5));
    }

    private static void setMemoryItem(VmInstanceInventory vm, Element parent) {
        Element memory = parent.addElement("Item");
        long size = vm.getMemorySize();
        memory.addElement("rasd:AllocationUnits").addText("byte");
        memory.addElement("rasd:Description").addText("Memory Size");
        memory.addElement("rasd:ElementName")
                .addText(String.format("%s %s of memory", SizeUnit.BYTE.toMegaByte((double) size),
                        SizeUnit.MEGABYTE.getName()
                ));
        memory.addElement("rasd:InstanceID").addText("2");
        memory.addElement("rasd:ResourceType").addText("4");
        memory.addElement("rasd:VirtualQuantity").addText(Long.toString(size));
    }

    private static void setCpuItem(VmInstanceInventory vm, Element parent) {
        Element cpu = parent.addElement("Item");
        int cpuNum = vm.getCpuNum();
        cpu.addElement("rasd:AllocationUnits").addText("hertz * 10^6");
        cpu.addElement("rasd:Description").addText("Number of Virtual CPUs");
        cpu.addElement("rasd:ElementName").addText(String.format("%d virtual CPU(s)", cpuNum));
        cpu.addElement("rasd:InstanceID").addText("1");
        cpu.addElement("rasd:ResourceType").addText("3");
        cpu.addElement("rasd:VirtualQuantity").addText(Integer.toString(cpuNum));
        cpu.addElement("vmw:CoresPerSocket")
                .addAttribute("ovf:required", "false")
                .addText(Integer.toString(cpuNum));
    }

    private static void setSystemSection(Element parent) {
        Element system = parent.addElement("System");
        system.addElement("vssd:ElementName").addText("Virtual Hardware Family");
        system.addElement("vssd:InstanceID").addText("0");
    }

    private static void setOperatingSystemSection(VmInstanceInventory vm, Element parent) {
        Map<String, Map<String, String>> map = getOsTypeMap(EXPORT_OS_TYPE_MAPPER_PATH);
        String guestOs = vm.getGuestOsType();
        Map<String, String> guestOsMap = map.get(guestOs);
        String osId = "102";
        String osType = "otherGuest64";
        if (guestOsMap != null) {
            osId = guestOsMap.get("vmwType");
            osType = guestOsMap.get("vmwOsName");
        }
        Element operatingSystem = parent.addElement("OperatingSystemSection")
                .addAttribute("ovf:id", osId)
                .addAttribute("vmw:osType", osType);
        operatingSystem.addElement("Info").addText("The operating system installed");
        operatingSystem.addElement("Description").addText(guestOs != null ? guestOs : "Generic OS");
    }

    private static void setDiskSection(List<VolumeInventory> volumes, Element envelope, List<OvfDiskInfo> diskInfos) {
        Element disks = envelope.addElement("DiskSection", XML_NAMESPACE_URI);
        disks.addElement("Info").addText("List of the virtual disks");
        for (int i = 0; i < volumes.size(); i++) {
            VolumeInventory volume = volumes.get(i);
            OvfDiskInfo diskInfo = diskInfos.get(i);
            String diskId = String.format("vmdisk%d", i + 1);
            disks.addElement("Disk")
                    .addAttribute("ovf:fileRef", diskInfo.getFileName())
                    .addAttribute("ovf:diskId", diskId)
                    .addAttribute("ovf:capacityAllocationUnits", "byte")
                    .addAttribute("ovf:capacity", Long.toString(volume.getSize()))
                    .addAttribute("ovf:format",
                            "http://www.vmware.com/interfaces/specifications/vmdk.html#streamOptimized"
                    );
            diskInfo.setDiskId(diskId);
        }
    }

    private static List<OvfEthernetAdapterInfo> setNetworkSection(List<VmNicInventory> nics, Element envelope) {
        Element networks = envelope.addElement("NetworkSection", XML_NAMESPACE_URI);
        networks.addElement("Info").addText("The list of logical networks");
        List<OvfEthernetAdapterInfo> networkInfos = new ArrayList<>();
        if (nics == null || nics.isEmpty()) {
            return networkInfos;
        }
        for (VmNicInventory nic : nics) {
            String netName = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.uuid, nic.getL3NetworkUuid())
                    .select(L3NetworkVO_.name)
                    .findValue();
            networks.addElement("Network")
                    .addAttribute("ovf:name", netName)
                    .addElement("Description")
                    .addText(String.format("The %s network", netName));
            OvfEthernetAdapterInfo net = new OvfEthernetAdapterInfo();
            net.setNetworkName(netName);
            net.setNicModel("E1000");
            networkInfos.add(net);
        }
        return networkInfos;
    }

    private static Element getOvfEnvelope(Document ovf) {
        Element envelope = ovf.addElement("Envelope");
        envelope.addNamespace("", XML_NAMESPACE_URI)
                .addNamespace("cim", "http://schemas.dmtf.org/wbem/wscim/1/common")
                .addNamespace("ovf", "http://schemas.dmtf.org/ovf/envelope/1")
                .addNamespace("vmw", "http://www.vmware.com/schema/ovf")
                .addNamespace("rasd",
                        "http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData"
                )
                .addNamespace("vssd",
                        "http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_VirtualSystemSettingData"
                );
        return envelope;
    }

    private static List<OvfDiskInfo> setReferences(int volumeNum, String vmUuid, Element parent) {
        if (volumeNum == 0) {
            throw new CloudOperationError(SysErrors.OPERATION_ERROR.toString(), "no volume can be exported.");
        }

        Element references = parent.addElement("References", XML_NAMESPACE_URI);
        List<OvfDiskInfo> diskInfos = new ArrayList<>();
        for (int i = 1; i <= volumeNum; i++) {
            String fileId = String.format("file%d", i);
            String fileRef = String.format("%s-disk-%d.vmdk", vmUuid, i);
            references.addElement("File")
                    .addAttribute("ovf:id", fileId)
                    .addAttribute("ovf:href", fileRef);
            OvfDiskInfo diskInfo = new OvfDiskInfo();
            diskInfos.add(diskInfo);
            diskInfo.setFileName(fileId);
            diskInfo.setFileRef(fileRef);
        }
        return diskInfos;
    }

    public static void fixReferences(Document ovf, List<ImageBackupStorageRefInventory> imageRefs) {
        Element root = ovf.getRootElement();
        Element references = root.element("References");
        int index = 0;
        for (Iterator<Element> it = references.elementIterator("File"); it.hasNext(); ++index) {
            ImageBackupStorageRefInventory imageRef = imageRefs.get(index);
            Element file = it.next();
            file.addAttribute("ovf:href", StringUtils.substringAfterLast(imageRef.getExportUrl(), "/"));
        }
    }
}
