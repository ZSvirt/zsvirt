package org.zstack.billing.spendingcalculator.pcidevice

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "PCI设备价格关系清单"

	field {
		name "priceUuid"
		desc ""
		type "String"
		since "2.4"
	}
	field {
		name "pciDeviceOfferingUuid"
		desc ""
		type "String"
		since "2.4"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.4"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.4"
	}
}
