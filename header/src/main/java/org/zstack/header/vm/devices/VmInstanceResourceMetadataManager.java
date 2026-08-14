package org.zstack.header.vm.devices;

import org.zstack.header.errorcode.ErrorCode;

import java.util.List;
import java.util.Map;

public interface VmInstanceResourceMetadataManager {
    String MEM_BALLOON_UUID = "4780bf6d2fa65700f22e36c27e8ff05c";

    String RESOURCE_CONFIG_UUID = "65700f22e34780bf6d2fa6c27e8ff05c";

    String GUEST_TOOLS_RESOURCE_CONFIG_UUID = "1a4ca3d92f9f43ba8291e2f60502fe62";

    int MEMORY_VOLUME_DEVICE_ID = Integer.MAX_VALUE;
    
    /**
     * create or update vm device address,
     * if no VmInstanceResourceMetadataVO with current resource, a new
     * record will be created, else update the existing one.
     *
     * @param resourceUuid uuid of resource need record device address
     * @param deviceAddress a instance of deviceAddressConfig record device address
     * @param vmInstanceUuid vm uuid of resource need record device address
     * @param metadata a string of anything request to be record with the device
     * @param metadataClass the canonical class name of metadata
     * @return VmInstanceResourceMetadataVO result vo of device address
     */
    VmInstanceResourceMetadataVO createOrUpdateVmResourceMetadata(String resourceUuid, DeviceAddress deviceAddress, String vmInstanceUuid, String metadata, String metadataClass);

    /**
     * create or update vm device address,
     * if no VmInstanceResourceMetadataVO with current resource, a new
     * record will be created, else update the existing one.
     *
     * @param virtualDeviceInfo contains resourceUuid and deviceInfo a structure oriented method
     * @param vmInstanceUuid vm uuid of resource
     * @return VmInstanceResourceMetadataVO result vo of device address
     */
    VmInstanceResourceMetadataVO createOrUpdateVmResourceMetadata(VirtualDeviceInfo virtualDeviceInfo, String vmInstanceUuid);

    /**
     * Save VM XML configuration metadata
     * Creates a new metadata record if none exists, otherwise updates the existing one
     *
     * @param vmXml          the XML configuration string of the VM
     * @param vmInstanceUuid VM instance UUID for which the XML should be saved
     * @throws IllegalArgumentException if vmXml is null or empty, or vmInstanceUuid is invalid
     */
    void saveVmXmlMetadata(String vmXml, String vmInstanceUuid);

    /**
     * get vm device address
     *
     * @param resourceUuid the uuid of resource that want to get device address
     * @param vmInstanceUuid vm uuid of resource
     * @return DeviceAddressConfig device address of resourceUuid
     */
    DeviceAddress getVmDeviceAddress(String resourceUuid, String vmInstanceUuid);

    /**
     * delete vm device address
     *
     * @param resourceUuid the uuid of resource that want to delete device address
     * @param vmInstanceUuid vm uuid of resource
     * @return ErrorCode if success it is null else not
     */
    ErrorCode deleteVmResourceMetadata(String resourceUuid, String vmInstanceUuid);

    /**
     * delete vm device address
     *
     * @param resourceUuid the uuid of resource that want to delete device address
     * @return ErrorCode if success it is null else not
     */
    ErrorCode deleteVmResourceMetadata(String resourceUuid);

    /**
     * delete vm related all devices' address
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address
     * @return ErrorCode if success it is null else not
     */
    ErrorCode deleteAllResourceMetadataByVm(String vmInstanceUuid);

    /**
     * modify virtio ,pci address is modify, need vm clean related devices
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address
     * @return ErrorCode if success it is null else not
     */

    ErrorCode deleteResourceMetadataByVmModifyVirtIO(String vmInstanceUuid);

    /**
     * archive current device address
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address
     * @param archiveForResourceUuid this uuid will be used to mark those vm related
     *                               address as a group. note: do not use a duplicate
     *                               archiveForResourceUuid to confuse yourself
     * @return VmInstanceResourceMetadataGroupVO the group marked by archiveForResourceUuid
     * and has references with all vm current related address
     */
    VmInstanceResourceMetadataGroupVO archiveCurrentResourceMetadata(String vmInstanceUuid, String archiveForResourceUuid);

    /**
     * revert current vm device address to a specific device
     * address group
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address
     * @param archiveForResourceUuid this uuid will be used to find a specific group
     *                               of device address
     * @return List<VmInstanceResourceMetadataVO> a list of vm device address
     */
    List<VmInstanceResourceMetadataVO> revertResourceMetadataFromArchive(String vmInstanceUuid, String archiveForResourceUuid);

    List<VmInstanceResourceMetadataVO> revertExistingDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid);

    List<VmInstanceResourceMetadataVO> revertRequestedDeviceAddressFromArchive(String vmInstanceUuid, String archiveForResourceUuid, List<String> needRevertResourceUuidList);
    /**
     * create device address from archive
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address and create device address for
     * @param archiveForResourceUuid this uuid will be used to find a specific group
     *                               of device address
     * @param resourceMap resource map will be used for uuid mapping, for example if new vm use uuidA to mark the
     *                    first disk which use uuidB, resourceMap.put(uuidB, uuidA), when create device address,
     *                    address with uuidB will be used to create a record with uuidA
     * @return List<VmInstanceResourceMetadataVO> a list of vm device address
     */
    List<VmInstanceResourceMetadataVO> createResourceMetadataFromArchive(String vmInstanceUuid, String archiveForResourceUuid, Map<String, String> resourceMap);

    /**
     * delete archive device address group
     *
     * @param archiveForResourceUuid this uuid will be used to find a specific group
     *                               of device address
     */
    void deleteArchiveVmInstanceResourceMetadataGroup(String archiveForResourceUuid);

    /**
     * get archive info from archiveForResourceUuid
     *
     * @param vmInstanceUuid vm uuid will be used to find related device address
     * @param archiveForResourceUuid this uuid will be used to find a specific group
     *                               of device address
     * @param metadataClass the canonical class name of metadata
     * @return List<VmInstanceResourceMetadataArchiveVO> a list of vm archive device address
     */
    List<VmInstanceResourceMetadataArchiveVO> getArchivedResourceMetadataInfoFromArchiveForResourceUuid(String vmInstanceUuid, String archiveForResourceUuid, String metadataClass);

    void updateVmResourceMetadataDeviceAddress(String vmInstanceUuid, String resourceUuid, String deviceAddress);
}
