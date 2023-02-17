package org.drools.compiler.integrationtests.alarms;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

public class OpenNMSAlarmHandlingTest {
    DroolsContext droolsContext = new DroolsContext();
    long tickLengthMillis = TimeUnit.MINUTES.toMillis(2);

    @Before
    public void setUp() {
        droolsContext.start();
    }

    @Test
    public void canDriveScenario() {
        try {
            final long start = 0;
            final long end = TimeUnit.MINUTES.toMillis(30);

            droolsContext.getClock().advanceTime(start, TimeUnit.MILLISECONDS);
            droolsContext.tick();

            //doAlarmFlapScenario();
            doCreateSituationScenario();

            // Tick every hour for the next week
            tickAtRateUntil(TimeUnit.HOURS.toMillis(1),
                    end + TimeUnit.DAYS.toMillis(1),
                    end + TimeUnit.DAYS.toMillis(8));
        } catch (Exception e) {
            throw e;
        }
    }

    private void tickAtRateUntil(long tickLength, long start, long end) {
        // Now keep tick'ing at an accelerated rate for another week
        for (long now = start; now <= end; now += tickLength) {
            // Tick
            droolsContext.getClock().advanceTime(tickLength, TimeUnit.MILLISECONDS);
            droolsContext.tick();
            // Ger results
        }
    }

    private void doCreateSituationScenario() {
        /*
                .withNodeDownEvent(1, 1)
                .withNodeDownEvent(2, 2)
                // Create a situation that contains the node down alarms
                .withSituationForNodeDownAlarms(3, "situation#1", 1, 2)
                // Now clear the node down alarms
                .withNodeUpEvent(4, 1)
                .withNodeUpEvent(4, 2)
         */
        Alarm node1DownAlarm = new Alarm();
        node1DownAlarm.setId(1);
        node1DownAlarm.setType(1);
        node1DownAlarm.setTimestamp(droolsContext.getClock().getCurrentTime());
        node1DownAlarm.setSeverity(4);
        node1DownAlarm.setLastEventSeverity(4);
        node1DownAlarm.setLastEventTime(node1DownAlarm.getTimestamp());
        node1DownAlarm.setReductionKey("nodeDown::1");
        droolsContext.insertOrUpdateAlarm(node1DownAlarm);

        // Tick
        droolsContext.getClock().advanceTime(tickLengthMillis, TimeUnit.MILLISECONDS);
        droolsContext.tick();

        Alarm node2DownAlarm = new Alarm();
        node2DownAlarm.setId(2);
        node2DownAlarm.setType(1);
        node2DownAlarm.setTimestamp(droolsContext.getClock().getCurrentTime());
        node2DownAlarm.setSeverity(4);
        node2DownAlarm.setLastEventSeverity(4);
        node2DownAlarm.setLastEventTime(node2DownAlarm.getTimestamp());
        node2DownAlarm.setReductionKey("nodeDown::2");
        droolsContext.insertOrUpdateAlarm(node2DownAlarm);

        // Tick
        droolsContext.getClock().advanceTime(tickLengthMillis, TimeUnit.MILLISECONDS);
        droolsContext.tick();

        Alarm situation = new Alarm();
        situation.setId(3);
        situation.setType(1);
        situation.setTimestamp(droolsContext.getClock().getCurrentTime());
        situation.setLastEventTime(situation.getTimestamp());
        situation.setReductionKey("situation::3");
        situation.setRelatedAlarmIds(Arrays.asList(1L, 2L));
        droolsContext.insertOrUpdateAlarm(situation);

        // Tick
        droolsContext.getClock().advanceTime(tickLengthMillis, TimeUnit.MILLISECONDS);
        droolsContext.tick();

        // Verify
        situation = getAlarmById(3);
        assertThat(situation.getSeverity()).isEqualTo(5);

        Alarm node1UpAlarm = new Alarm();
        node1UpAlarm.setId(4);
        node1UpAlarm.setType(2);
        node1UpAlarm.setTimestamp(droolsContext.getClock().getCurrentTime());
        node1UpAlarm.setSeverity(2);
        node1UpAlarm.setLastEventSeverity(2);
        node1UpAlarm.setLastEventTime(node1UpAlarm.getTimestamp());
        node1UpAlarm.setReductionKey("nodeUp::4");
        node1UpAlarm.setClearKey("nodeDown::1");
        droolsContext.insertOrUpdateAlarm(node1UpAlarm);

        // Tick
        droolsContext.getClock().advanceTime(tickLengthMillis, TimeUnit.MILLISECONDS);
        droolsContext.tick();

        Alarm node2UpAlarm = new Alarm();
        node2UpAlarm.setId(5);
        node2UpAlarm.setType(2);
        node2UpAlarm.setTimestamp(droolsContext.getClock().getCurrentTime());
        node2UpAlarm.setSeverity(2);
        node2UpAlarm.setLastEventSeverity(2);
        node2UpAlarm.setLastEventTime(node2UpAlarm.getTimestamp());
        node2UpAlarm.setReductionKey("nodeUp::5");
        node2UpAlarm.setClearKey("nodeDown::2");
        droolsContext.insertOrUpdateAlarm(node2UpAlarm);

        // Tick
        droolsContext.getClock().advanceTime(tickLengthMillis, TimeUnit.MILLISECONDS);
        droolsContext.tick();

        // Verify
        node1DownAlarm = getAlarmById(1);
        node2DownAlarm = getAlarmById(2);
        situation = getAlarmById(3);
        assertThat(node1DownAlarm.getSeverity()).isEqualTo(2);
        assertThat(node2DownAlarm.getSeverity()).isEqualTo(2);
        assertThat(situation.getSeverity()).isEqualTo(2);

    }

    private void doAlarmFlapScenario() {
        /*
          .withNodeDownEvent(step, 1)
            .withNodeUpEvent(2*step, 1)
            .withNodeDownEvent(3*step, 1)
            .withNodeUpEvent(4*step, 1)
            .withNodeDownEvent(5*step, 1)
         */

        Alarm nodeDownAlarm = new Alarm();
        nodeDownAlarm.setId(1);
        nodeDownAlarm.setType(1);
        nodeDownAlarm.setTimestamp(droolsContext.getClock().getCurrentTime());
        nodeDownAlarm.setSeverity(4);
        nodeDownAlarm.setLastEventSeverity(4);
        nodeDownAlarm.setLastEventTime(nodeDownAlarm.getTimestamp());
        nodeDownAlarm.setReductionKey("nodeDown::1");
        droolsContext.insertOrUpdateAlarm(nodeDownAlarm);

        // Tick
        droolsContext.getClock().advanceTime(tickLengthMillis, TimeUnit.MILLISECONDS);
        droolsContext.tick();

        // Verify
        assertThat(getAlarmById(1).getSeverity()).isEqualTo(4);

        // nodeUp to clear
        Alarm nodeUpAlarm = new Alarm();
        nodeUpAlarm.setId(2);
        nodeUpAlarm.setType(2);
        nodeUpAlarm.setTimestamp(droolsContext.getClock().getCurrentTime());
        nodeUpAlarm.setSeverity(2);
        nodeUpAlarm.setLastEventSeverity(2);
        nodeUpAlarm.setLastEventTime(nodeUpAlarm.getTimestamp());
        nodeUpAlarm.setReductionKey("nodeUp::1");
        nodeUpAlarm.setClearKey("nodeDown::1");
        droolsContext.insertOrUpdateAlarm(nodeUpAlarm);

        // Tick
        droolsContext.getClock().advanceTime(tickLengthMillis, TimeUnit.MILLISECONDS);
        droolsContext.tick();

        // Verify
        nodeDownAlarm = getAlarmById(1);
        assertThat(nodeDownAlarm.getSeverity()).isEqualTo(2);

        // nodeDown again to unclear
        nodeDownAlarm.setTimestamp(droolsContext.getClock().getCurrentTime());
        nodeDownAlarm.setLastEventTime(droolsContext.getClock().getCurrentTime() + 1);
        nodeDownAlarm.setReductionKey("nodeDown::1");
        droolsContext.insertOrUpdateAlarm(nodeDownAlarm);

        // Tick
        droolsContext.getClock().advanceTime(tickLengthMillis, TimeUnit.MILLISECONDS);
        droolsContext.tick();

        // Verify
        nodeDownAlarm = getAlarmById(1);
        assertThat(nodeDownAlarm.getSeverity()).isEqualTo(4);

        // nodeUp again to re-clear
        nodeUpAlarm.setTimestamp(droolsContext.getClock().getCurrentTime());
        nodeUpAlarm.setLastEventTime(nodeUpAlarm.getTimestamp());
        droolsContext.insertOrUpdateAlarm(nodeDownAlarm);

        // Tick
        droolsContext.getClock().advanceTime(tickLengthMillis, TimeUnit.MILLISECONDS);
        droolsContext.tick();

        // Verify
        nodeDownAlarm = getAlarmById(1);
        assertThat(nodeDownAlarm.getSeverity()).isEqualTo(2);

        // nodeDown again to unclear
        nodeDownAlarm.setTimestamp(droolsContext.getClock().getCurrentTime());
        nodeDownAlarm.setLastEventTime(droolsContext.getClock().getCurrentTime() + 1);
        nodeDownAlarm.setReductionKey("nodeDown::1");
        droolsContext.insertOrUpdateAlarm(nodeDownAlarm);

        // Tick
        droolsContext.getClock().advanceTime(tickLengthMillis, TimeUnit.MILLISECONDS);
        droolsContext.tick();

        // Verify
        nodeDownAlarm = getAlarmById(1);
        assertThat(nodeDownAlarm.getSeverity()).isEqualTo(4);
    }

    private Alarm getAlarmById(long id) {
        return droolsContext.getAlarmById(id);
    }
}
