package org.drools.compiler.integrationtests.alarms;

import org.kie.api.runtime.rule.FactHandle;

public class AlarmAndFact {
    private Alarm alarm;
    private FactHandle fact;

    public AlarmAndFact(Alarm alarm, FactHandle fact) {
        this.alarm = alarm;
        this.fact = fact;
    }

    public Alarm getAlarm() {
        return alarm;
    }

    public FactHandle getFact() {
        return fact;
    }

    @Override
    public String toString() {
        return "AlarmAndFact{" +
                "alarm=" + alarm +
                ", fact=" + fact +
                '}';
    }
}
