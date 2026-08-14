package org.zstack.pluginpremium.externalapiadapter.typeconvertor;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: fubang
 * @Date: 2018/5/18
 */
public class SecurityGroupProtocol {
	public static final List<String> validProtocols = Arrays.asList("TCP","UDP","ICMP","ALL");

	public static String toZstack(String value) {
		value = value.toUpperCase();
		if (validProtocols.contains(value)) {
			return value;
		}
		throw new CloudRuntimeException(String.format("Not support IpProtocol[value: %s]", value));
	}

	public static String toEcs(String value){
		if (validProtocols.contains(value)) {
			return value.toLowerCase();
		}
		throw new CloudRuntimeException(String.format("Not support IpProtocol[value: %s]", value));
	}

}
