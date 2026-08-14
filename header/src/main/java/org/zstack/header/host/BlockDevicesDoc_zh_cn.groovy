package org.zstack.header.host

import org.zstack.header.host.BlockDevices.BlockDevice

doc {

	title "物理机所有磁盘信息"

	ref {
		name "unusedBlockDevices"
		path "org.zstack.header.host.BlockDevices.unusedBlockDevices"
		desc "没有分区且没有挂载目录或者所有分区没有挂载目录的磁盘"
		type "List"
		since "zsv 4.3.0"
		clz BlockDevice.class
	}
	ref {
		name "usedBlockDevices"
		path "org.zstack.header.host.BlockDevices.usedBlockDevices"
		desc "已挂载目录或者有分区挂载目录的磁盘"
		type "List"
		since "zsv 4.3.0"
		clz BlockDevice.class
	}
}
