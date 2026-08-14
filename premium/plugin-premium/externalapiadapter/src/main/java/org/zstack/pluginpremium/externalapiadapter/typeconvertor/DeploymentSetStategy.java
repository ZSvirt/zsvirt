package org.zstack.pluginpremium.externalapiadapter.typeconvertor;

import org.zstack.header.exception.CloudRuntimeException;

public enum DeploymentSetStategy {
	LOOSEDISPERSION("LooseDispersion", "antiSoft"),
	STRICTDISPERSION("StrictDispersion", "antiHard");

	public static DeploymentSetStategy getDeploymentSetStategyFromEcs(String value){
		for (DeploymentSetStategy deploymentSetStategy:DeploymentSetStategy.values()) {
			if (deploymentSetStategy.ecsValue.equalsIgnoreCase(value)){
				return deploymentSetStategy;
			}
		}

		throw new CloudRuntimeException(String.format("'Strategy'[value: %s] is not valid", value));
	}

	public static DeploymentSetStategy getDeploymentSetStategyFromZstack(String value){
		for (DeploymentSetStategy deploymentSetStategy:DeploymentSetStategy.values()) {
			if (deploymentSetStategy.zstackValue.equalsIgnoreCase(value)){
				return deploymentSetStategy;
			}
		}

		throw new CloudRuntimeException(String.format("'policy'[value: %s] is not valid", value));
	}

	public final String ecsValue;
	public final String zstackValue;

	DeploymentSetStategy(String ecsValue, String zstackValue){
		this.ecsValue = ecsValue;
		this.zstackValue = zstackValue;
	}
}
