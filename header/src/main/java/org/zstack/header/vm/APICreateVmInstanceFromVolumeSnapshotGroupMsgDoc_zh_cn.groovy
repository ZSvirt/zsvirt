package org.zstack.header.vm

import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotGroupEvent

doc {
	title "CreateVmInstanceFromVolumeSnapshotGroup"

	category "vmInstance"

	desc """从快照组创建云主机"""

	rest {
		request {
			url "POST /v1/vm-instances/from/volume-snapshots/group/{volumeSnapshotGroupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmInstanceFromVolumeSnapshotGroupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "zsv 3.11.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "instanceOfferingUuid"
					enclosedIn "params"
					desc "计算规格UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "cpuNum"
					enclosedIn "params"
					desc "CPU数量"
					location "body"
					type "Integer"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "memorySize"
					enclosedIn "params"
					desc "内存大小"
					location "body"
					type "Long"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc "三层网络UUID"
					location "body"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "云主机类型"
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
					values ("UserVm","ApplianceVm")
				}
				column {
					name "volumeSnapshotGroupUuid"
					enclosedIn "params"
					desc "快照组UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.11.0"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "primaryStorageUuidForRootVolume"
					enclosedIn "params"
					desc "根盘的主存储UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn "params"
					desc "默认三层网络UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "strategy"
					enclosedIn "params"
					desc "创建策略"
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
					values ("InstantStart","CreateStopped")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "rootVolumeSystemTags"
					enclosedIn "params"
					desc "根云盘的系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "dataVolumeSystemTags"
					enclosedIn "params"
					desc "数据云盘的系统标签，键值为快照UUID"
					location "body"
					type "Map"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "reservedMemorySize"
					enclosedIn "params"
					desc ""
					location "body"
					type "Long"
					optional true
					since "zsv 3.17.21"
				}
				column {
					name "vmNicParams"
					enclosedIn "params"
					desc "网卡信息"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.10"
				}
				column {
					name "resetTpm"
					enclosedIn "params"
					desc "是否重置 TPM 状态"
					location "body"
					type "Boolean"
					optional true
					since "zsv 5.0.0"
				}
			}
		}

		response {
			clz APICreateVmInstanceFromVolumeSnapshotGroupEvent.class
		}
	}
}