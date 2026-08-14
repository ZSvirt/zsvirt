package org.zstack.header.network.l3

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l3.datatypes.IpCapacityData
import org.zstack.header.errorcode.ErrorCode

doc {

	title "IP地址容量返回值"

	ref {
		name "error"
		path "org.zstack.header.network.l3.APIGetIpAddressCapacityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "totalCapacity"
		desc "IP地址容量"
		type "long"
		since "zsv 0.6"
	}
	field {
		name "availableCapacity"
		desc "可用IP地址容量"
		type "long"
		since "zsv 0.6"
	}
	field {
		name "usedIpAddressNumber"
		desc "已使用IP数量"
		type "long"
		since "zsv 3.1"
	}
	field {
		name "ipv4TotalCapacity"
		desc "IPv4地址容量"
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
		desc "已使用IPv4数量"
		type "long"
		since "zsv 3.10"
	}
	field {
		name "ipv6TotalCapacity"
		desc "IPv6地址容量"
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
		desc "已使用IPv6数量"
		type "long"
		since "zsv 3.10"
	}
	ref {
		name "capacityData"
		path "org.zstack.header.network.l3.APIGetIpAddressCapacityReply.capacityData"
		desc "所有被查询的资源的IP地址容量信息"
		type "List"
		since "zsv 3.9.0"
		clz IpCapacityData.class
	}
	field {
		name "resourceType"
		desc "所查询资源的类型（地址范围、三层网络、区域）"
		type "String"
		since "zsv 3.9.0"
	}
	field {
		name "success"
		desc "成功"
		type "boolean"
		since "zsv 0.6"
	}
}
