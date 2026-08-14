package org.zstack.header.image



doc {

	title "上传镜像任务详情"

	field {
		name "longJobUuid"
		desc "长任务UUID"
		type "String"
		since "zsv 3.11.0"
	}
	field {
		name "longJobState"
		desc "长任务状态"
		type "String"
		since "zsv 3.11.0"
	}
	field {
		name "imageUuid"
		desc "镜像UUID"
		type "String"
		since "zsv 3.11.0"
	}
	field {
		name "imageUploadUrl"
		desc "镜像上传URL"
		type "String"
		since "zsv 3.11.0"
	}
	field {
		name "offset"
		desc "上传偏移量，单位为 Byte"
		type "long"
		since "zsv 3.11.0"
	}
}
