package org.zstack.network.hostNetworkInterface

doc {

	title "物理网卡设备清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.5.0"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "bondingUuid"
		desc "Bond UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "interfaceModel"
		desc "网卡型号"
		type "String"
		since "3.17.11"
	}
	field {
		name "vendorId"
		desc "产商Id"
		type "String"
		since "3.17.11"
	}
	field {
		name "deviceId"
		desc "设备Id"
		type "String"
		since "3.17.11"
	}
	field {
		name "subvendorId"
		desc "子产商Id"
		type "String"
		since "3.17.11"
	}
	field {
		name "subdeviceId"
		desc "子设备Id"
		type "String"
		since "3.17.11"
	}
	field {
		name "interfaceName"
		desc "网卡名称"
		type "String"
		since "3.5.0"
	}
	field {
		name "interfaceType"
		desc "网卡应用状态，有nomaster、bridgeSlave、bondSlave"
		type "String"
		since "3.5.0"
	}
	field {
		name "speed"
		desc "网卡速率"
		type "Long"
		since "3.5.0"
	}
	field {
		name "slaveActive"
		desc "Bond链路状态"
		type "Boolean"
		since "3.5.0"
	}
	field {
		name "carrierActive"
		desc "物理链路状态"
		type "Boolean"
		since "3.5.0"
	}
	field {
		name "ipAddresses"
		desc "IP地址"
		type "List"
		since "3.5.0"
	}
	field {
		name "gateway"
		desc "网关地址"
		type "String"
		since "3.17.0"
	}
	field {
		name "mac"
		desc "MAC地址"
		type "String"
		since "3.5.0"
	}
	field {
		name "callBackIp"
		desc "回调地址"
		type "String"
		since "3.17.0"
	}
	field {
		name "pciDeviceAddress"
		desc "网卡PCI地址"
		type "String"
		since "3.5.0"
	}
	field {
		name "offloadStatus"
		desc ""
		type "String"
		since "3.17.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.17.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.5.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.5.0"
	}
}
