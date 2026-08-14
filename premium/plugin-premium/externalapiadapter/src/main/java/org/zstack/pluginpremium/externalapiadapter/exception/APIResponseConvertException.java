package org.zstack.pluginpremium.externalapiadapter.exception;

/**
 * Created by lining on 2018/5/08.
 */
public class APIResponseConvertException extends RuntimeException {
	String ecsAttributeName;

	String msg;

	public APIResponseConvertException(String ecsAttributeName, String msg) {
		this.ecsAttributeName = ecsAttributeName;
		this.msg = msg;
	}

	public String getEcsAttributeName() {
		return ecsAttributeName;
	}

	public void setEcsAttributeName(String ecsAttributeName) {
		this.ecsAttributeName = ecsAttributeName;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
