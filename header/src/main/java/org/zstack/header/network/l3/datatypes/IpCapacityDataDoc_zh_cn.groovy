package org.zstack.header.network.l3.datatypes



doc {

	title "资源的IP地址容量信息结构"

	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "zsv 3.9.0"
	}
	field {
		name "totalCapacity"
		desc "IP地址总容量"
		type "long"
		since "zsv 3.9.0"
	}
	field {
		name "availableCapacity"
		desc "可用IP地址容量"
		type "long"
		since "zsv 3.9.0"
	}
	field {
		name "usedIpAddressNumber"
		desc "已用IP地址容量"
		type "long"
		since "zsv 3.9.0"
	}
	field {
		name "ipv4TotalCapacity"
		desc "IPv4地址总容量"
		type "long"
		since "zsv 3.10"
	}
	field {
		name "ipv4AvailableCapacity"
		desc "可用IPv4地址容量"
		type "long"
		since "zsv 3.10"
	}
	field {
		name "ipv4UsedIpAddressNumber"
		desc "已用IPv4地址容量"
		type "long"
		since "zsv 3.10"
	}
	field {
		name "ipv6TotalCapacity"
		desc "IPv6地址总容量"
		type "long"
		since "zsv 3.10"
	}
	field {
		name "ipv6AvailableCapacity"
		desc "可用IPv6地址容量"
		type "long"
		since "zsv 3.10"
	}
	field {
		name "ipv6UsedIpAddressNumber"
		desc "已用IPv6地址容量"
		type "long"
		since "zsv 3.10"
	}
}
