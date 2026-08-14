package org.zstack.mttyDevice



doc {

	title "MTTY 设备虚拟状态"

	field {
		name "VFIO_MDEV_VIRTUALIZABLE"
		desc "支持 VFIO_MDEV 虚拟化"
		type "MttyDeviceVirtStatus"
		since "3.15.11"
	}
	field {
		name "VFIO_MDEV_VIRTUALIZED"
		desc "已 VFIO_MDEV 虚拟化"
		type "MttyDeviceVirtStatus"
		since "3.15.11"
	}
	field {
		name "UNVIRTUALIZABLE"
		desc "不可虚拟化"
		type "MttyDeviceVirtStatus"
		since "3.15.11"
	}
	field {
		name "UNKNOWN"
		desc "未知"
		type "MttyDeviceVirtStatus"
		since "3.15.11"
	}
}
