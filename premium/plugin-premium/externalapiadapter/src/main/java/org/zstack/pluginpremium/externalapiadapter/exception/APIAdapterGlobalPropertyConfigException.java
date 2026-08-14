package org.zstack.pluginpremium.externalapiadapter.exception;

/**
 * Created by lining on 2018/5/8.
 */
public class APIAdapterGlobalPropertyConfigException extends Exception {

	String propertyName;

	String message;

	public APIAdapterGlobalPropertyConfigException(String propertyName, String message) {
		this.propertyName = propertyName;
		this.message = message;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
