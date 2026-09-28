package org.zstack.pciDevice.header.virtual.vfio_mdev



doc {

	title "MDEV设备启用状态"

	field {
		name "Enabled"
		desc "启用"
		type "MdevDeviceState"
		since "zsv_3.5.0"
	}
	field {
		name "Disabled"
		desc "停用"
		type "MdevDeviceState"
		since "zsv_3.5.0"
	}
}
