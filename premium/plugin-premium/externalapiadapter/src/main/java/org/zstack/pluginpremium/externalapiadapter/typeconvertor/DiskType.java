package org.zstack.pluginpremium.externalapiadapter.typeconvertor;

import org.zstack.header.exception.CloudRuntimeException;

public enum DiskType {
	SYSTEM("system", "Root", false),
	DATA("data", "Data", true);

	public static final DiskType[] DISK_TYPES = DiskType.values();

	public static DiskType getDiskTypeFromEcs(String value){
		for (DiskType diskType: DISK_TYPES) {
			if (diskType.ecsValue.equalsIgnoreCase(value)){
				return diskType;
			}
		}

		throw new CloudRuntimeException(String.format("'DiskType'[value: %s] is not valid", value));
	}

	public static DiskType getDiskTypeFromZstack(String value){
		for (DiskType diskType: DISK_TYPES) {
			if (diskType.zstackValue.equalsIgnoreCase(value)){
				return diskType;
			}
		}

		throw new CloudRuntimeException(String.format("'type'[value: %s] is not valid", value));
	}

	public static DiskType getDiskTypeFromPortable(String portable) {
		boolean value = Boolean.parseBoolean(portable);
		for (DiskType diskType : DISK_TYPES) {
			if (diskType.portable == value) {
				return diskType;
			}
		}
		throw new CloudRuntimeException(String.format("'portable'[value: %s] is not valid", portable));
	}

	public final String ecsValue;

	public final String zstackValue;

	public final boolean portable;

	DiskType(String ecsValue, String zstackValue, boolean portable) {
		this.ecsValue = ecsValue;
		this.zstackValue = zstackValue;
		this.portable = portable;
	}
}
