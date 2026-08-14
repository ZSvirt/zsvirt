package org.zstack.header.vm

import org.zstack.header.vm.APIFlattenVmInstanceEvent

doc {
	title "FlattenVmInstance"

	category "vmInstance"

	desc """扁平合并云主机"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIFlattenVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "flattenVmInstance"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "full"
					enclosedIn "flattenVmInstance"
					desc "扁平合并云主机上所有云盘"
					location "body"
					type "boolean"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "dryRun"
					enclosedIn "flattenVmInstance"
					desc "试运行，可用于预测数据用量"
					location "body"
					type "boolean"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
			}
		}

		response {
			clz APIFlattenVmInstanceEvent.class
		}
	}
}