package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APICreateVmFromVmBackupEvent

doc {
	title "CreateVmFromVmBackup"

	category "backup.volume"

	desc """从云主机备份创建虚拟机"""

	rest {
		request {
			url "POST /v1/vm-instances/from/vm-backups/{groupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmFromVmBackupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "groupUuid"
					enclosedIn "params"
					desc "云主机备份组UUID"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "params"
					desc "镜像存储UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "instanceOfferingUuid"
					enclosedIn "params"
					desc "计算规格UUID"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc "一组三层网络的UUID"
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "vmNicParams"
					enclosedIn "params"
					desc "网卡信息"
					location "body"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "云主机类型"
					location "body"
					type "String"
					optional true
					since "3.0.0"
					values ("UserVm","ApplianceVm")
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "primaryStorageUuidForRootVolume"
					enclosedIn "params"
					desc "根云盘主存储UUID"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "primaryStorageUuidForDataVolume"
					enclosedIn "params"
					desc "数据盘主存储UUID"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "rootVolumeSystemTags"
					enclosedIn "params"
					desc "根云盘系统标签"
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "dataVolumeSystemTags"
					enclosedIn "params"
					desc "数据盘系统标签"
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "diskAOs"
					enclosedIn "params"
					desc "按云盘维度传递参数，DiskAO.sourceUuid 与备份对应的原始云盘UUID (VolumeBackupVO.volumeUuid) 绑定；可为每块云盘单独指定 primaryStorageUuid、systemTags 等。注意：diskAOs 与 primaryStorageUuidForRootVolume/primaryStorageUuidForDataVolume、rootVolumeSystemTags/dataVolumeSystemTags 互斥，不能同时指定"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn "params"
					desc "默认三层网络的UUID"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
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
					name "cpuNum"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "zsv 4.3.0"
				}
				column {
					name "memorySize"
					enclosedIn "params"
					desc ""
					location "body"
					type "Long"
					optional true
					since "zsv 4.3.0"
				}
				column {
					name "reservedMemorySize"
					enclosedIn "params"
					desc ""
					location "body"
					type "Long"
					optional true
					since "zsv 4.3.0"
				}
				column {
					name "resetTpm"
					enclosedIn "params"
					desc "创建的虚拟机是否重置 TPM 状态"
					location "body"
					type "Boolean"
					optional true
					since "zsv 5.0.3"
				}
			}
		}

		response {
			clz APICreateVmFromVmBackupEvent.class
		}
	}
}
