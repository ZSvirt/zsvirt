package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APICreateVmFromVolumeBackupEvent

doc {
	title "CreateVmFromVolumeBackup"

	category "backup.volume"

	desc """从备份创建云主机"""

	rest {
		request {
			url "POST /v1/vm-instances/from/vm-backup/{backupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmFromVolumeBackupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "云主机名字"
					location "body"
					type "String"
					optional false
					since "3.18.0"
				}
				column {
					name "backupUuid"
					enclosedIn "params"
					desc "备份UUID"
					location "url"
					type "String"
					optional false
					since "3.18.0"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "params"
					desc "镜像存储UUID"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "instanceOfferingUuid"
					enclosedIn "params"
					desc "计算规格UUID"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "cpuNum"
					enclosedIn "params"
					desc "虚拟机 CPU 核心数"
					location "body"
					type "Integer"
					optional true
					since "4.10.28"
				}
				column {
					name "memorySize"
					enclosedIn "params"
					desc "虚拟机内存大小, 单位 byte"
					location "body"
					type "Long"
					optional true
					since "4.10.28"
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn "params"
					desc "默认L3网络UUID"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc "一组三层网络的UUID"
					location "body"
					type "List"
					optional false
					since "3.18.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "云主机类型"
					location "body"
					type "String"
					optional true
					since "3.18.0"
					values ("UserVm","ApplianceVm")
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "primaryStorageUuidForRootVolume"
					enclosedIn "params"
					desc "根云盘主存储UUID"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "rootVolumeSystemTags"
					enclosedIn "params"
					desc "根云盘系统标签"
					location "body"
					type "List"
					optional true
					since "3.18.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.18.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.18.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.18.0"
				}
				column {
					name "strategy"
					enclosedIn "params"
					desc "云主机创建策略"
					location "body"
					type "String"
					optional true
					since "4.10.0"
					values ("InstantStart","JustCreate","CreateStopped")
				}
				column {
					name "resetTpm"
					enclosedIn "params"
					desc "创建的虚拟机是否重置 TPM 状态"
					location "body"
					type "Boolean"
					optional true
					since "5.0.0"
				}
			}
		}

		response {
			clz APICreateVmFromVolumeBackupEvent.class
		}
	}
}