package org.drools.compiler.integrationtests.alarms;

import java.util.Date;

public interface AlarmService {

    void clearAlarm(Alarm alarm, Date now);
    void unclearAlarm(Alarm alarm, Date now);
    void deleteAlarm(Alarm alarm);
    void setSeverity(Alarm alarm, long severity, Date now);

}
