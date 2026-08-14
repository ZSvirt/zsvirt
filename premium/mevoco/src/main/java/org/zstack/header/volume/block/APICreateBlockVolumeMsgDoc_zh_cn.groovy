package org.zstack.header.volume.block

import org.zstack.header.volume.block.APICreateBlockVolumeEvent

doc {
	title "CreateBlockVolume"

	category "mevoco"

	desc """创建块存储卷"""

	rest {
		request {
			url "POST /v1/block-volumes"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateBlockVolumeMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "size"
					enclosedIn "params"
					desc "容量"
					location "body"
					type "Long"
					optional false
					since "3.17.11"
				}
				column {
					name "primaryStorageUuid"
					enclosedIn "params"
					desc "主存储UUID"
					location "body"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "accessPathId"
					enclosedIn "params"
					desc "访问路径id"
					location "body"
					type "Integer"
					optional true
					since "3.17.11"
				}
				column {
					name "accessPathIqn"
					enclosedIn "params"
					desc "访问路径iqn"
					location "body"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "burstTotalBw"
					enclosedIn "params"
					desc "突发带宽"
					location "body"
					type "Long"
					optional true
					since "3.17.11"
				}
				column {
					name "burstTotalIops"
					enclosedIn "params"
					desc "突发IOPS"
					location "body"
					type "Long"
					optional true
					since "3.17.11"
				}
				column {
					name "maxTotalBw"
					enclosedIn "params"
					desc "最大带宽"
					location "body"
					type "Long"
					optional true
					since "3.17.11"
				}
				column {
					name "maxTotalIops"
					enclosedIn "params"
					desc "最大IOPS"
					location "body"
					type "Long"
					optional true
					since "3.17.11"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "protocol"
					enclosedIn "params"
					desc "卷所属协议"
					location "body"
					type "String"
					optional true
					since "4.10.16"
				}
			}
		}

		response {
			clz APICreateBlockVolumeEvent.class
		}
	}
}