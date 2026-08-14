package org.zstack.header.host

import org.zstack.header.host.BlockDevices.BlockDevice

doc {

	title "物理机单个磁盘信息"

	field {
		name "name"
		desc "硬盘设备名字"
		type "String"
		since "zsv 4.3.0"
	}
	field {
		name "type"
		desc "磁盘类型"
		type "String"
		since "zsv 4.3.0"
	}
	field {
		name "size"
		desc "磁盘容量"
		type "long"
		since "zsv 4.3.0"
	}
	field {
		name "used"
		desc "磁盘已使用容量"
		type "long"
		since "zsv 4.10.0"
	}
	field {
		name "available"
		desc "磁盘可使用容量"
		type "long"
		since "zsv 4.10.0"
	}
	field {
		name "physicalSector"
		desc "物理扇区大小"
		type "long"
		since "zsv 4.3.0"
	}
	field {
		name "logicalSector"
		desc "逻辑扇区大小"
		type "long"
		since "zsv 4.3.0"
	}
	field {
		name "mountPoint"
		desc "挂载点"
		type "String"
		since "zsv 4.3.0"
	}
	ref {
		name "children"
		path "org.zstack.header.host.BlockDevices.BlockDevice.children"
		desc "子分区"
		type "List"
		since "zsv 4.3.0"
		clz BlockDevice.class
	}
	field {
		name "partitionTable"
		desc "分区表类型"
		type "String"
		since "zsv 4.3.0"
	}
	field {
		name "fsType"
		desc "文件系统类型"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "serialNumber"
		desc "序列号"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "model"
		desc "型号"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "mediaType"
		desc "介质类型"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "usedRatio"
		desc "使用率"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "smartPassed"
		desc "获取磁盘smart状态"
		type "Boolean"
		since "zsv 4.10.0"
	}
	field {
		name "smartMessage"
		desc "获取磁盘smart状态失败时输出信息"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "multipath"
		desc "是否为多路径设备；true 表示该磁盘自身为 multipath 设备或为 multipath 底层路径盘"
		type "boolean"
		since "zsv 5.0.0"
	}
}
