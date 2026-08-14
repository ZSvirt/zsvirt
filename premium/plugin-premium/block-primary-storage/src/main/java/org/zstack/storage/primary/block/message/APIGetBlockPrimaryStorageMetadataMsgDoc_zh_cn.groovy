package org.zstack.storage.primary.block.message

import org.zstack.storage.primary.block.message.APIQueryBlockPrimaryStorageReply

doc {
	title "GetBlockPrimaryStorageMetadata"

	category "storage.primary"

	desc """获取Block存储的元数据"""

	rest {
		request {
			url "POST /v1/primary-storage/block/metadata"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetBlockPrimaryStorageMetadataMsg.class

			desc """"""

			params {

				column {
					name "vendorName"
					enclosedIn "param"
					desc "Block存储厂商名称"
					location "body"
					type "String"
					optional false
					since "3.15.11"
				}
				column {
					name "metadata"
					enclosedIn "param"
					desc "存储元数据（ip，port，用户名、密码）"
					location "body"
					type "String"
					optional false
					since "3.15.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.15.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.15.11"
				}
			}
		}

		response {
			clz APIQueryBlockPrimaryStorageReply.class
		}
	}
}