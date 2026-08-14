package org.zstack.header.vm

import java.lang.Long
import java.lang.Integer
import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.vm.VmNicInventory
import org.zstack.header.volume.VolumeInventory
import org.zstack.header.vm.cdrom.VmCdRomInventory

doc {

	title "虚拟机实例清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "zoneUuid"
		desc "区域 UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "clusterUuid"
		desc "集群 UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "imageUuid"
		desc "镜像 UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "hostUuid"
		desc "主机 UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "lastHostUuid"
		desc "上一次启动的主机 UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "instanceOfferingUuid"
		desc "计算规格 UUID, 已废弃"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "rootVolumeUuid"
		desc "根硬盘 UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "platform"
		desc "虚拟机的平台, 是 Windows / Linux 还是其它"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "architecture"
		desc "虚拟机架构, 是 x86_64 / aarch64 还是其它"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "defaultL3NetworkUuid"
		desc "默认使用的三层网络 UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "type"
		desc "虚拟机类型, 是用户使用虚拟机还是平台特殊用途的虚拟机"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "hypervisorType"
		desc "虚拟机虚拟化类型, 一般为 KVM"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "memorySize"
		desc "内存大小"
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "cpuNum"
		desc "CPU 数量"
		type "Integer"
		since "zsv 0.6"
	}
	field {
		name "cpuSpeed"
		desc "CPU 速度"
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "allocatorStrategy"
		desc "虚拟机分配策略"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "state"
		desc "虚拟机是否启用的状态"
		type "String"
		since "zsv 0.6"
	}
	ref {
		name "vmNics"
		path "org.zstack.header.vm.VmInstanceInventory.vmNics"
		desc "虚拟机网卡清单列表"
		type "List"
		since "zsv 0.6"
		clz VmNicInventory.class
	}
	ref {
		name "allVolumes"
		path "org.zstack.header.vm.VmInstanceInventory.allVolumes"
		desc "虚拟机硬盘清单列表"
		type "List"
		since "zsv 0.6"
		clz VolumeInventory.class
	}
	ref {
		name "vmCdRoms"
		path "org.zstack.header.vm.VmInstanceInventory.vmCdRoms"
		desc "虚拟机 CDRom 清单列表"
		type "List"
		since "zsv 0.6"
		clz VmCdRomInventory.class
	}
	field {
		name "guestOsType"
		desc "虚拟机宿主操作系统类型"
		type "String"
		since "zsv 3.11.2"
	}
}
