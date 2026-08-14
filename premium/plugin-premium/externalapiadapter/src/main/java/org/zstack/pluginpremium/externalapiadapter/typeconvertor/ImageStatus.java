package org.zstack.pluginpremium.externalapiadapter.typeconvertor;

import org.zstack.header.exception.CloudRuntimeException;

public enum ImageStatus {
	CREATING("Creating","Creating"),
	AVAILABLE("Available", "Ready"),
	UnAvailable("UnAvailable", "Downloading,Migrating"),
	CreateFailed("CreateFailed", "Error");

	public static ImageStatus getImageStatusFromEcs(String value){
		for(ImageStatus imageStatus: ImageStatus.values()){
			if (imageStatus.ecsValue.equalsIgnoreCase(value)){
				return imageStatus;
			}
		}

		throw new CloudRuntimeException(String.format("'Status'[value: %s] is not valid", value));
	}

	public static ImageStatus getImageStatusFromZstack(String value){
		for (ImageStatus imageStatus: ImageStatus.values()){
			if (imageStatus.zstackValue.contains(value)){
				return imageStatus;
			}
		}
		throw new CloudRuntimeException(String.format("'status'[value: %s] is not valid", value));
	}

	public final String ecsValue;
	public final String zstackValue;

	ImageStatus(String ecsValue, String zstackValue){
		this.ecsValue = ecsValue;
		this.zstackValue = zstackValue;
	}
}
