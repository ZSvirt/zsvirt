package org.zstack.pluginpremium.externalapiadapter.typeconvertor;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.sdk.EipInventory;

public enum EipStatus {
    INUSE("InUse", "vmNicUuid not null"),
    AVAILABLE("Available", "vmNicUuid is null"),
    ASSOCIATING("Associating", "Associating"),
    UNASSOCIATING("Unassociating", "Unassociating");

    public final String ecsValue;
    public final String zstackValue;

    EipStatus(String ecsValue, String zstackValue) {
        this.ecsValue = ecsValue;
        this.zstackValue = zstackValue;
    }

	public static EipStatus getEipStatusFromEcs(String value) {
		for (EipStatus eipStatus : EipStatus.values()) {
			if (eipStatus.ecsValue.equalsIgnoreCase(value)) {
				return eipStatus;
			}
		}

		throw new CloudRuntimeException(String.format("'Status'[value: %s] is not valid", value));
	}

	public static EipStatus getEipStatusFromZstack(EipInventory inventory) {
		if (inventory.vmNicUuid == null) {
			return AVAILABLE;
		} else {
			return INUSE;
		}
	}

    public static EipStatus getEipStatusFromTag(String tag) {
        if (tag != null) {
            for (EipStatus status : EipStatus.values()) {
                if (tag.equals(status.zstackValue)) {
                    return status;
                }
            }
        }
        throw new CloudRuntimeException(String.format("'status'[value: %s] is not valid", tag));
    }
}
