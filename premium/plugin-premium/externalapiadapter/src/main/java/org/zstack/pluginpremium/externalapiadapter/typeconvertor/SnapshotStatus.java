package org.zstack.pluginpremium.externalapiadapter.typeconvertor;

import org.zstack.header.exception.CloudRuntimeException;

public enum SnapshotStatus {
	PROGRESSING("progressing","Creating"),
	ACCOMPLISHED("accomplished", "Ready"),
	FAILED("failed", null),
	ALL("all", null);

	public static SnapshotStatus getSnapshotStatusFromEcs(String value){
		for (SnapshotStatus snapshotStatus: SnapshotStatus.values()){
			if (snapshotStatus.ecsValue.equalsIgnoreCase(value)){
				return snapshotStatus;
			}
		}

		throw new CloudRuntimeException(String.format("'Status'[value: %s] is not valid", value));
	}

	public static SnapshotStatus getSnapshotStatusFromZstack(String value){
		if (value != null){
			for (SnapshotStatus snapshotStatus: SnapshotStatus.values()){
				if (value.equalsIgnoreCase(snapshotStatus.zstackValue)){
					return snapshotStatus;
				}
			}
		}

		throw new CloudRuntimeException(String.format("'status'[value: %s] is not valid", value));
	}

	public final String ecsValue;
	public final String zstackValue;

	SnapshotStatus(String ecsValue, String zstackValue){
		this.ecsValue = ecsValue;
		this.zstackValue = zstackValue;
	}
}
