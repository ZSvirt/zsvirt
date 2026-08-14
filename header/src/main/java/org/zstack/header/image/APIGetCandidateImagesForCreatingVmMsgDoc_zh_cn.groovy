package org.zstack.header.image

import org.zstack.header.image.APIGetCandidateImagesForCreatingVmReply

doc {
	title "GetCandidateImagesForCreatingVm"

	category "host.allocator"

	desc """获取用于创建云主机的候选镜像"""

	rest {
		request {
			url "GET /v1/images/primaryStorage/{primaryStorageUuid}/candidate-image"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateImagesForCreatingVmMsg.class

			desc """"""

			params {

				column {
					name "primaryStorageUuid"
					enclosedIn ""
					desc "主存储UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.1.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.1.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.1.1"
				}
			}
		}

		response {
			clz APIGetCandidateImagesForCreatingVmReply.class
		}
	}
}