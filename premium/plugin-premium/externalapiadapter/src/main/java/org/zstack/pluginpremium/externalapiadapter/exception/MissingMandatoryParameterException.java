package org.zstack.pluginpremium.externalapiadapter.exception;

/**
 * Created by lining on 2018/5/08.
 */
public class MissingMandatoryParameterException extends Exception {

	private String parameterName;

	public MissingMandatoryParameterException(String parameterName) {
		this.parameterName = parameterName;
	}

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }
}
