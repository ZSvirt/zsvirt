package org.zstack.pluginpremium.externalapiadapter.exception;

import org.zstack.sdk.ErrorCode;

/**
 * Created by lining on 2018/5/08.
 */
public class InvalidParameterException extends Exception {

	String parameterName;

	ErrorCode errorCode;

	public InvalidParameterException(String parameterName, ErrorCode errorCode) {
		this.parameterName = parameterName;
		this.errorCode = errorCode;
	}

	public String getParameterName() {
		return parameterName;
	}

	public void setParameterName(String parameterName) {
		this.parameterName = parameterName;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}
}
