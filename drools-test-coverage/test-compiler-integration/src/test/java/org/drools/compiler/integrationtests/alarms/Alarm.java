package org.drools.compiler.integrationtests.alarms;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Alarm implements Serializable {
    private long id = 0;
    private String name = "";
    private int type = 0;
    private long timestamp = 0;
    private long severity = 0;
    private long lastEventSeverity = 0;
    private String reductionKey;
    private String clearKey;
    private long lastEventTime = 0;
    private List<Long> relatedAlarmIds = new LinkedList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getSeverity() {
        return severity;
    }

    public void setSeverity(long severity) {
        this.severity = severity;
    }

    public long getLastEventSeverity() {
        return lastEventSeverity;
    }

    public void setLastEventSeverity(long lastEventSeverity) {
        this.lastEventSeverity = lastEventSeverity;
    }

    public String getReductionKey() {
        return reductionKey;
    }

    public void setReductionKey(String reductionKey) {
        this.reductionKey = reductionKey;
    }

    public String getClearKey() {
        return clearKey;
    }

    public void setClearKey(String clearKey) {
        this.clearKey = clearKey;
    }

    public long getLastEventTime() {
        return lastEventTime;
    }

    public void setLastEventTime(long lastEventTime) {
        this.lastEventTime = lastEventTime;
    }

    public List<Long> getRelatedAlarmIds() {
        return relatedAlarmIds;
    }

    public void setRelatedAlarmIds(List<Long> relatedAlarmIds) {
        this.relatedAlarmIds = relatedAlarmIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alarm alarm = (Alarm) o;
        return id == alarm.id && type == alarm.type && timestamp == alarm.timestamp && severity == alarm.severity && lastEventSeverity == alarm.lastEventSeverity && lastEventTime == alarm.lastEventTime && Objects.equals(name, alarm.name) && Objects.equals(reductionKey, alarm.reductionKey) && Objects.equals(clearKey, alarm.clearKey) && Objects.equals(relatedAlarmIds, alarm.relatedAlarmIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, timestamp, severity, lastEventSeverity, reductionKey, clearKey, lastEventTime, relatedAlarmIds);
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", severity=" + severity +
                ", lastEventSeverity=" + lastEventSeverity +
                ", reductionKey='" + reductionKey + '\'' +
                ", clearKey='" + clearKey + '\'' +
                ", lastEventTime=" + lastEventTime +
                ", relatedAlarmIds=" + relatedAlarmIds +
                '}';
    }

    public Alarm clone() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (Alarm) ois.readObject();
        } catch (IOException|ClassNotFoundException e) {
            return null;
        }
    }

}
