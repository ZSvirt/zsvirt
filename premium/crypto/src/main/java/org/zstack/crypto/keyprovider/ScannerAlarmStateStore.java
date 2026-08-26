package org.zstack.crypto.keyprovider;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.zwatch.alarm.scanner.ScannerAlarmStateVO;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ScannerAlarmStateStore {
    public enum AlarmState {
        ACTIVE, INACTIVE
    }

    private static final String ACTIVE = "Active";
    private static final String INACTIVE = "Inactive";

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Autowired
    private DatabaseFacade dbf;

    public AlarmState getAlarmState(String scannerName, String alarmType, String alarmKey) {
        String cacheKey = cacheKey(scannerName, alarmType, alarmKey);
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return toAlarmState(cached);
        }

        String dbState = SQL.New("select s.state from ScannerAlarmStateVO s" +
                        " where s.scannerName = :scannerName and s.alarmType = :alarmType and s.alarmKey = :alarmKey",
                String.class)
                .param("scannerName", scannerName)
                .param("alarmType", alarmType)
                .param("alarmKey", alarmKey)
                .find();
        if (dbState == null) {
            cache.put(cacheKey, INACTIVE);
            return AlarmState.INACTIVE;
        }
        cache.put(cacheKey, dbState);
        return toAlarmState(dbState);
    }

    public boolean isActive(String scannerName, String alarmType, String alarmKey) {
        return getAlarmState(scannerName, alarmType, alarmKey) == AlarmState.ACTIVE;
    }

    public void markActive(String scannerName, String alarmType, String alarmKey) {
        updateState(scannerName, alarmType, alarmKey, ACTIVE);
    }

    public void markInactive(String scannerName, String alarmType, String alarmKey) {
        updateState(scannerName, alarmType, alarmKey, INACTIVE);
    }

    public void hydrate(String scannerName, String alarmType) {
        String prefix = cachePrefix(scannerName, alarmType);
        cache.keySet().removeIf(key -> key.startsWith(prefix));

        List<ScannerAlarmStateVO> rows = SQL.New("select s from ScannerAlarmStateVO s" +
                        " where s.scannerName = :scannerName and s.alarmType = :alarmType",
                ScannerAlarmStateVO.class)
                .param("scannerName", scannerName)
                .param("alarmType", alarmType)
                .list();
        for (ScannerAlarmStateVO vo : rows) {
            cache.put(cacheKey(scannerName, alarmType, vo.getAlarmKey()), vo.getState());
        }
    }

    public Set<String> activeAlarmKeys(String scannerName, String alarmType) {
        String prefix = cachePrefix(scannerName, alarmType);
        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            if (entry.getKey().startsWith(prefix) && ACTIVE.equals(entry.getValue())) {
                keys.add(entry.getKey().substring(prefix.length()));
            }
        }
        return keys;
    }

    public void evictInactiveNotIn(String scannerName, String alarmType, Set<String> retainedAlarmKeys) {
        String prefix = cachePrefix(scannerName, alarmType);
        cache.entrySet().removeIf(entry -> {
            if (!entry.getKey().startsWith(prefix) || !INACTIVE.equals(entry.getValue())) {
                return false;
            }
            String alarmKey = entry.getKey().substring(prefix.length());
            return !retainedAlarmKeys.contains(alarmKey);
        });
    }

    private void updateState(String scannerName, String alarmType, String alarmKey, String state) {
        String cacheKey = cacheKey(scannerName, alarmType, alarmKey);
        if (state.equals(cache.get(cacheKey))) {
            return;
        }

        ScannerAlarmStateVO vo = SQL.New("select s from ScannerAlarmStateVO s" +
                        " where s.scannerName = :scannerName and s.alarmType = :alarmType and s.alarmKey = :alarmKey",
                ScannerAlarmStateVO.class)
                .param("scannerName", scannerName)
                .param("alarmType", alarmType)
                .param("alarmKey", alarmKey)
                .find();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (vo == null) {
            vo = new ScannerAlarmStateVO();
            vo.setUuid(Platform.getUuid());
            vo.setScannerName(scannerName);
            vo.setAlarmType(alarmType);
            vo.setAlarmKey(alarmKey);
            vo.setState(state);
            vo.setLastUpdateTime(now);
            dbf.persist(vo);
        } else {
            vo.setState(state);
            vo.setLastUpdateTime(now);
            dbf.update(vo);
        }
        cache.put(cacheKey, state);
    }

    private AlarmState toAlarmState(String state) {
        if (ACTIVE.equals(state)) {
            return AlarmState.ACTIVE;
        }
        return AlarmState.INACTIVE;
    }

    private String cachePrefix(String scannerName, String alarmType) {
        return scannerName + "|" + alarmType + "|";
    }

    private String cacheKey(String scannerName, String alarmType, String alarmKey) {
        return cachePrefix(scannerName, alarmType) + alarmKey;
    }
}
