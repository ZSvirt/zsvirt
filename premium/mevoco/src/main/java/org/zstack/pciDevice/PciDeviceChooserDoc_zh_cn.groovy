package org.zstack.pciDevice



doc {

	title "该 PCI 设备被占用的类型"

	field {
		name "None"
		desc "表示该 PCI 设备未被占用"
		type "PciDeviceChooser"
		since "3.18.0"
	}
	field {
		name "Device"
		desc "表示该 PCI 设备被某个虚拟机通过指定的方式占用"
		type "PciDeviceChooser"
		since "3.18.0"
	}
	field {
		name "Spec"
		desc "表示该 PCI 设备被某个虚拟机通过规格的方式占用"
		type "PciDeviceChooser"
		since "3.18.0"
	}
}
