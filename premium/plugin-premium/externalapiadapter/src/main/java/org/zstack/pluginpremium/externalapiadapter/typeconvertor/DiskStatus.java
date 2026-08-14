package org.zstack.pluginpremium.externalapiadapter.typeconvertor;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.sdk.VolumeInventory;

import java.util.List;

public enum DiskStatus {
	CREATING("Creating", "Creating"),
	// In_use -> Ready + vmInstanceUuid not null, Ready -> In_use or Available
	INUSER("In_use", "Ready"),
	// Available -> Ready or NotInstantiated (vmInstanceUuid is null), NotInstantiated -> Available
	AVAILABLE("Available", "NotInstantiated"),
	ATTACHING("Attaching", null),
	DETACHING("Detaching", null),
	REINITING("ReIniting", null),
	ALL("All", null);

	private static DiskStatus getDiskStatusFromEcs(String value){
		for(DiskStatus diskStatus: DiskStatus.values()){
			if (diskStatus.ecsValue.equalsIgnoreCase(value)){
				return diskStatus;
			}
		}

		throw new CloudRuntimeException(String.format("'Status'[value: %s] is not valid", value));
	}

	private static DiskStatus getDiskStatusFromZstack(String value){
		if (value != null){
			for(DiskStatus diskStatus: DiskStatus.values()){
				if (value.equalsIgnoreCase(diskStatus.zstackValue)){
					return diskStatus;
				}
			}
		}

		throw new CloudRuntimeException(String.format("'status'[value: %s] is not valid", value));
	}

	public static DiskStatus addConditionsWhenQuery(List conditions, String value){
		DiskStatus diskStatus = getDiskStatusFromEcs(value);

		if (INUSER.equals(diskStatus)){
			conditions.add("vmInstanceUuid not null");
		}else if (AVAILABLE.equals(diskStatus)){
			conditions.add("vmInstanceUuid is null");
			return ALL;
		}

		return diskStatus;
	}

	public static String convertDiskStatusZstackIntoEcs(VolumeInventory inventory){
		DiskStatus diskStatus = getDiskStatusFromZstack(inventory.status);
		String status = diskStatus.ecsValue;

		if (INUSER.equals(diskStatus) && inventory.vmInstanceUuid == null){
			status = AVAILABLE.ecsValue;
		}

		return status;
	}

	public final String ecsValue;

	public final String zstackValue;

	DiskStatus(String ecsValue, String zstackValue) {
		this.ecsValue = ecsValue;
		this.zstackValue = zstackValue;
	}

}
