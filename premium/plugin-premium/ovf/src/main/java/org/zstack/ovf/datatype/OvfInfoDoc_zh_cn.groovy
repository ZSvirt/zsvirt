package org.zstack.ovf.datatype

import org.zstack.ovf.datatype.OvfDiskInfo
import org.zstack.ovf.datatype.OvfNetworkInfo
import org.zstack.ovf.datatype.OvfCpuInfo
import org.zstack.ovf.datatype.OvfMemoryInfo
import org.zstack.ovf.datatype.OvfOSInfo
import org.zstack.ovf.datatype.OvfSystemInfo
import org.zstack.ovf.datatype.OvfEthernetAdapterInfo
import org.zstack.ovf.datatype.OvfCdDriverInfo
import org.zstack.ovf.datatype.OvfVolumeInfo

doc {

	title "OVF模板信息"

	ref {
		name "disks"
		path "org.zstack.ovf.datatype.OvfInfo.disks"
		desc "磁盘信息"
		type "List"
		since "3.14.6"
		clz OvfDiskInfo.class
	}
	ref {
		name "networks"
		path "org.zstack.ovf.datatype.OvfInfo.networks"
		desc "网络信息列表"
		type "List"
		since "3.14.6"
		clz OvfNetworkInfo.class
	}
	ref {
		name "cpu"
		path "org.zstack.ovf.datatype.OvfInfo.cpu"
		desc "CPU 信息"
		type "OvfCpuInfo"
		since "3.14.6"
		clz OvfCpuInfo.class
	}
	ref {
		name "memory"
		path "org.zstack.ovf.datatype.OvfInfo.memory"
		desc "内存信息"
		type "OvfMemoryInfo"
		since "3.14.6"
		clz OvfMemoryInfo.class
	}
	field {
		name "vmName"
		desc "云主机名称"
		type "String"
		since "3.14.6"
	}
	ref {
		name "os"
		path "org.zstack.ovf.datatype.OvfInfo.os"
		desc "操作系统信息"
		type "OvfOSInfo"
		since "3.14.6"
		clz OvfOSInfo.class
	}
	ref {
		name "systemInfo"
		path "org.zstack.ovf.datatype.OvfInfo.systemInfo"
		desc "硬件系统信息"
		type "OvfSystemInfo"
		since "3.14.6"
		clz OvfSystemInfo.class
	}
	ref {
		name "nics"
		path "org.zstack.ovf.datatype.OvfInfo.nics"
		desc "网卡信息"
		type "List"
		since "3.14.6"
		clz OvfEthernetAdapterInfo.class
	}
	ref {
		name "cdDrivers"
		path "org.zstack.ovf.datatype.OvfInfo.cdDrivers"
		desc "光驱信息"
		type "List"
		since "3.14.6"
		clz OvfCdDriverInfo.class
	}
	ref {
		name "volumes"
		path "org.zstack.ovf.datatype.OvfInfo.volumes"
		desc "磁盘信息"
		type "List"
		since "3.14.6"
		clz OvfVolumeInfo.class
	}
}
