package org.zstack.autoscaling.template

import org.zstack.autoscaling.template.APICreateAutoScalingTemplateEvent

doc {
	title "CreateAutoScalingVmTemplate"

	category "autoscaling"

	desc """创建伸缩组云主机模块"""

	rest {
		request {
			url "POST /v1/autoscaling/vmtemplate"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAutoScalingVmTemplateMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceName"
					enclosedIn "params"
					desc "云主机名称"
					location "body"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "vmInstanceDescription"
					enclosedIn "params"
					desc "云主机描述"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "vmInstanceOfferingUuid"
					enclosedIn "params"
					desc "云主机实例规则"
					location "body"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "imageUuid"
					enclosedIn "params"
					desc "云主机镜像UUID"
					location "body"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc "云主机三层网络列表"
					location "body"
					type "List"
					optional false
					since "3.1.0"
				}
				column {
					name "rootDiskOfferingUuid"
					enclosedIn "params"
					desc "云主机根云盘规格"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "dataDiskOfferingUuids"
					enclosedIn "params"
					desc "数据盘规格列表"
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "vmInstanceZoneUuid"
					enclosedIn "params"
					desc "云主机所属地区"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "vmInstanceClusterUuid"
					enclosedIn "params"
					desc "云主机所属集群"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "primaryStorageUuidForRootVolume"
					enclosedIn "params"
					desc "根云盘主存储UUID"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn "params"
					desc "云主机默认三层网络"
					location "body"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "模板类型"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "vmInstanceType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("UserVm","ApplianceVm")
				}
				column {
					name "strategy"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("InstantStart","CreateStopped")
				}
			}
		}

		response {
			clz APICreateAutoScalingTemplateEvent.class
		}
	}
}