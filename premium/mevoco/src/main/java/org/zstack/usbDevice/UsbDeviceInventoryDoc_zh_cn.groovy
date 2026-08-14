package org.zstack.usbDevice

import org.zstack.usbDevice.UsbDeviceState
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "USB设备"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.2"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.2"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.2"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "2.2"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "2.2"
	}
	ref {
		name "state"
		path "org.zstack.usbDevice.UsbDeviceInventory.state"
		desc "USB设备状态"
		type "UsbDeviceState"
		since "2.2"
		clz UsbDeviceState.class
	}
	field {
		name "busNum"
		desc "总线号"
		type "String"
		since "2.2"
	}
	field {
		name "devNum"
		desc "设备号"
		type "String"
		since "2.2"
	}
	field {
		name "idVendor"
		desc "VendorID"
		type "String"
		since "2.2"
	}
	field {
		name "idProduct"
		desc "ProductID"
		type "String"
		since "2.2"
	}
	field {
		name "iManufacturer"
		desc "生产商"
		type "String"
		since "2.2"
	}
	field {
		name "iProduct"
		desc "设备类型"
		type "String"
		since "2.2"
	}
	field {
		name "iSerial"
		desc "序列号"
		type "String"
		since "2.2"
	}
	field {
		name "usbVersion"
		desc "USB版本"
		type "String"
		since "2.2"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.2"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.2"
	}
}
