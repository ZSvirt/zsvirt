package org.zstack.header.vm

import org.zstack.header.vm.APICreateVmInstanceEvent

doc {
	title "创建云主机(CreateVmInstance)"

	category "云主机"

	desc """创建一个新的云主机"""

	rest {
		request {
			url "POST /v1/vm-instances"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "云主机名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "instanceOfferingUuid"
					enclosedIn "params"
					desc "计算规格UUID。指定云主机的CPU、内存等参数。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "imageUuid"
					enclosedIn "params"
					desc "镜像UUID。云主机的根云盘会从该字段指定的镜像创建。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc "三层网络UUID列表。可以指定一个或多个三层网络，云主机会在每个网络上创建一个网卡。"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "vmNicParams"
					enclosedIn "params"
					desc "网卡信息"
					location "body"
					type "String"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "云主机类型。保留字段，无需指定。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("UserVm","ApplianceVm")
				}
				column {
					name "rootDiskOfferingUuid"
					enclosedIn "params"
					desc "根云盘规格UUID。如果`imageUuid`字段指定的镜像类型是ISO，该字段必须指定以确定需要创建的根云盘大小。如果镜像类型是非ISO，该字段无需指定。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "dataDiskSizes"
					enclosedIn "params"
					desc "自定义云盘大小列表。可以指定一个或多个云盘大小（可重复）为云主机创建一个或多个数据云盘。"
					location "body"
					type "List"
					optional true
					since "zsv 3.14.6"
				}
				column {
					name "dataDiskOfferingUuids"
					enclosedIn "params"
					desc "云盘规格UUID列表。可以指定一个或多个云盘规格UUID（UUID可以重复）为云主机创建一个或多个数据云盘。"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID。若指定，云主机会在指定区域创建。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID。若指定，云主机会在指定集群创建，该字段优先级高于`zoneUuid`。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID。若指定，云主机会在指定物理机创建，该字段优先级高于`zoneUuid`和`clusterUuid`。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "primaryStorageUuidForRootVolume"
					enclosedIn "params"
					desc "主存储UUID。若指定，云主机的根云盘会在指定主存储创建。"
					location "body"
					type "String"
					optional true
					since "zsv 1.8"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "云主机的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn "params"
					desc "默认三层网络UUID。当在`l3NetworkUuids`指定了多个三层网络时，该字段指定提供默认路由的三层网络。若不指定，`l3NetworkUuids`的第一个网络被选为默认网络。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，云主机会使用该字段值作为UUID。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.4"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "云主机系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "云主机用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "strategy"
					enclosedIn "params"
					desc "云主机创建策略,创建后立刻启动或创建后不启动"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("InstantStart","JustCreate","CreateStopped")
				}
				column {
					name "rootVolumeSystemTags"
					enclosedIn "params"
					desc "云主机根盘所需要的系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.0"
				}
				column {
					name "dataVolumeSystemTags"
					enclosedIn "params"
					desc "云主机数据盘所需要的系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.0"
				}
				column {
					name "cpuNum"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "zsv 3.4.0"
				}
				column {
					name "memorySize"
					enclosedIn "params"
					desc ""
					location "body"
					type "Long"
					optional true
					since "zsv 3.4.0"
				}
				column {
					name "rootDiskSize"
					enclosedIn "params"
					desc ""
					location "body"
					type "Long"
					optional true
					since "zsv 3.4.0"
				}
				column {
					name "dataVolumeSystemTagsOnIndex"
					enclosedIn "params"
					desc ""
					location "body"
					type "Map"
					optional true
					since "zsv 3.14.24"
				}
				column {
					name "sshKeyPairUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.17.21"
				}
				column {
					name "dataVolumeTemplateUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "diskAOs"
					enclosedIn "params"
					desc "boot=true表示diskAO用于设置云主机平台、架构、操作系统与根盘总线；diskAO.boot=false表示加载的硬盘"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "platform"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "guestOsType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "architecture"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.17.21"
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
					name "virtio"
					enclosedIn "params"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "allocatorStrategy"
					enclosedIn "params"
					desc "虚拟机分配策略"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.10"
				}
			}
		}

		response {
			clz APICreateVmInstanceEvent.class
		}
	}
}