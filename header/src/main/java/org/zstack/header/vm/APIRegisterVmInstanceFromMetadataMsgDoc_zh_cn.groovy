package org.zstack.header.vm

import org.zstack.header.vm.APIRegisterVmInstanceFromMetadataEvent

doc {
	title "从元数据注册云主机(RegisterVmInstanceFromMetadata)"

	category "云主机"

	desc """根据主存储上的元数据文件注册（恢复）一台云主机"""

	rest {
		request {
			url "POST /v1/vm-instances/metadata/register"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRegisterVmInstanceFromMetadataMsg.class

			desc """"""

			params {

				column {
					name "metadataPath"
					enclosedIn "params"
					desc "元数据文件在主存储上的路径"
					location "body"
					type "String"
					optional false
					since "zsv_5.0.0"
				}
				column {
					name "primaryStorageUuid"
					enclosedIn "params"
					desc "目标主存储UUID"
					location "body"
					type "String"
					optional false
					since "zsv_5.0.0"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "zsv_5.0.0"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional false
					since "zsv_5.0.0"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "zsv_5.0.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "云主机名称"
					location "body"
					type "String"
					optional true
					since "zsv_5.0.0"
				}
				column {
					name "regenerateUuid"
					enclosedIn "params"
					desc "是否修复虚拟机相关资源UUID；设置为true时，将为虚拟机及其卷、网卡、快照、快照树、快照组等相关资源重新生成无冲突UUID；默认为false，表示沿用元数据中的资源UUID"
					location "body"
					type "boolean"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv_5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv_5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv_5.0.0"
				}
			}
		}

		response {
			clz APIRegisterVmInstanceFromMetadataEvent.class
		}
	}
}