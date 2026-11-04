-- Feature: License support CPU core quota type | ZSV-10588

CALL INSERT_COLUMN('LicenseHistoryVO', 'quotaType', 'varchar(64)', 0, 'None', 'uuid');
CALL INSERT_COLUMN('LicenseHistoryVO', 'quota', 'int(10)', 0, 0, 'quotaType');

UPDATE LicenseHistoryVO
SET quotaType = CASE
    WHEN cpuNum IS NOT NULL AND cpuNum > 0 THEN 'CPUSocket'
    WHEN hostNum IS NOT NULL AND hostNum > 0 THEN 'Host'
    WHEN vmNum IS NOT NULL AND vmNum > 0 THEN 'VM'
    ELSE quotaType
END;

UPDATE LicenseHistoryVO
SET quota = CASE
    WHEN cpuNum IS NOT NULL AND cpuNum > 0 THEN cpuNum
    WHEN hostNum IS NOT NULL AND hostNum > 0 THEN hostNum
    WHEN vmNum IS NOT NULL AND vmNum > 0 THEN vmNum
    ELSE 0
END;

CALL DROP_COLUMN('LicenseHistoryVO', 'cpuNum');
CALL DROP_COLUMN('LicenseHistoryVO', 'hostNum');
CALL DROP_COLUMN('LicenseHistoryVO', 'vmNum');
CALL DROP_COLUMN('LicenseHistoryVO', 'capacity');

CALL INSERT_COLUMN('HostCapacityVO', 'cpuCoreNum', 'int unsigned', 0, '0', 'cpuSockets');

-- Others
