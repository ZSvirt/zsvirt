package org.zstack.header.vm

import org.zstack.header.vm.APIGetVirtualizerInfoReply

doc {
	title "GetVirtualizerInfo"

	category "mevoco"

	desc """获取资源虚拟化软件信息"""

	rest {
		request {
			url "GET /v1/vm-instances/virtualizer-info"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVirtualizerInfoMsg.class

			desc """当前用于查询 qemu 版本"""

			params {

				column {
					name "uuids"
					enclosedIn ""
					desc "VM / Host 的 uuid，可以混查"
					location "query"
					type "List"
					optional false
					since "3.16.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.16.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.16.21"
				}
			}
		}

		response {
			clz APIGetVirtualizerInfoReply.class
		}
	}
}