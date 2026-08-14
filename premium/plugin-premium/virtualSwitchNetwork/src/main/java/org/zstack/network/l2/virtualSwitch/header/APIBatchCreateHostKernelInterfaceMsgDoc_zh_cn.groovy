package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIBatchCreateHostKernelInterfaceEvent

doc {
	title "BatchCreateHostKernelInterface"

	category "network.l2"

	desc """批量创建Kernel适配器"""

	rest {
		request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/kernel-interfaces"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIBatchCreateHostKernelInterfaceMsg.class

			desc """"""

			params {

				column {
					name "structs"
					enclosedIn "params"
					desc "Kernel适配器结构体列表"
					location "body"
					type "List"
					optional false
					since "4.10.20"
				}
				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "trafficTypes"
					enclosedIn "params"
					desc "流量类型"
					location "body"
					type "List"
					optional true
					since "4.10.20"
					values ("Management","Storage")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
			}
		}

		response {
			clz APIBatchCreateHostKernelInterfaceEvent.class
		}
	}
}