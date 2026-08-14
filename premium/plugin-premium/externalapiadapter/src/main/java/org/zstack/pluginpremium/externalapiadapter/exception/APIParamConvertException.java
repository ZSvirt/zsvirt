package org.zstack.pluginpremium.externalapiadapter.exception;

import org.zstack.sdk.ErrorCode;

/**
 * Created by lining on 2018/4/26.
 */
public class APIParamConvertException extends RuntimeException {
	private String ecsParamName;

	private String msg;

	private ErrorCode errorCode;

	public APIParamConvertException(String ecsParamName, String msg) {
		this.ecsParamName = ecsParamName;
		this.msg = msg;
	}

	public APIParamConvertException(String ecsParamName, ErrorCode errorCode, String msg) {
		this.ecsParamName = ecsParamName;
		this.msg = msg;
		this.errorCode = errorCode;
	}

	public APIParamConvertException(String ecsParamName, ErrorCode code) {
		this.ecsParamName = ecsParamName;
		this.errorCode = code;
	}

	public String getEcsParamName() {
		return ecsParamName;
	}

	public void setEcsParamName(String ecsParamName) {
		this.ecsParamName = ecsParamName;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
