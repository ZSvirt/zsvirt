package org.zstack.pluginpremium.externalapiadapter.typeconvertor;

import org.zstack.header.exception.CloudRuntimeException;

public enum SecurityGroupDirection {
	ALL("all", null),
	EGRESS("egress", "Egress"),
	INGRESS("ingress", "Ingress");

	public static SecurityGroupDirection getSecurityGroupDirectionFromEcs(String value){
		for (SecurityGroupDirection securityGroupDirection: SecurityGroupDirection.values()){
			if (securityGroupDirection.ecsValue.equalsIgnoreCase(value)){
				return securityGroupDirection;
			}
		}

		throw new CloudRuntimeException(String.format("'Direction'[value: %s] is not valid", value));
	}

	public static SecurityGroupDirection getSecurityGroupDirectionFromZstack(String value){
		if (value != null){
			for (SecurityGroupDirection securityGroupDirection: SecurityGroupDirection.values()){
				if (value.equalsIgnoreCase(securityGroupDirection.zstackValue)){
					return securityGroupDirection;
				}
			}
		}

		throw new CloudRuntimeException(String.format("'status'[value: %s] is not valid", value));
	}

	public final String ecsValue;
	public final String zstackValue;

	SecurityGroupDirection(String ecsValue, String zstackValue){
		this.ecsValue = ecsValue;
		this.zstackValue = zstackValue;
	}
}
