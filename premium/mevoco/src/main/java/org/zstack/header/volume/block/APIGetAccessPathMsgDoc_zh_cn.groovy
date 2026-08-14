package org.zstack.header.volume.block

import org.zstack.header.volume.block.APIGetAccessPathReply

doc {
	title "GetAccessPath"

	category "mevoco"

	desc """获取访问路径列表"""

	rest {
		request {
			url "GET /v1/block-volumes/access/path"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAccessPathMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "primaryStorageUuid"
					enclosedIn ""
					desc "主存储UUID"
					location "query"
					type "String"
					optional false
					since "3.17.11"
				}
			}
		}

		response {
			clz APIGetAccessPathReply.class
		}
	}
}