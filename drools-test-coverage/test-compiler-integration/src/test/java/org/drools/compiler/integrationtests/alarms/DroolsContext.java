package org.drools.compiler.integrationtests.alarms;

import org.drools.core.ClockType;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.time.SessionPseudoClock;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class DroolsContext implements AlarmService {
    private static final Logger LOG = LoggerFactory.getLogger(DroolsContext.class);

    private final Map<Long, AlarmAndFact> alarmsById = new HashMap<>();

    private final String kSessionName = DroolsContext.class.getCanonicalName();

    private ReleaseId releaseIdForContainerUsedByKieSession;

    private KieContainer kieContainer;

    private KieSession kieSession;

    private Thread thread;

    private SessionPseudoClock clock;

    protected AtomicLong fireThreadId = new AtomicLong(-1);

    private final AtomicBoolean started = new AtomicBoolean(false);

    public void start() {
        // Build and deploy our ruleset
        final ReleaseId kieModuleReleaseId = buildKieModule();
        // Fire it up
        startWithModuleAndFacts(kieModuleReleaseId, Collections.emptyList());
    }

    private void startWithModuleAndFacts(ReleaseId releaseId, List<Object> factObjects) {
        final KieServices ks = KieServices.Factory.get();
        kieContainer = ks.newKieContainer(releaseId);
        kieSession = kieContainer.newKieSession(kSessionName);
        clock = kieSession.getSessionClock();
        // Add the clock to the session
        kieSession.insert(kieSession.getSessionClock());
        // Add the alarm service
        kieSession.setGlobal("alarmService", this);

        // Save the releaseId
        releaseIdForContainerUsedByKieSession = releaseId;

        // We're started!
        started.set(true);
    }


    private ReleaseId buildKieModule() {
        final KieServices ks = KieServices.Factory.get();
        final KieFileSystem kfs = ks.newKieFileSystem();
        final ReleaseId id = generateReleaseId();

        final KieModuleModel module = ks.newKieModuleModel();
        final KieBaseModel base = module.newKieBaseModel("test");
        base.setDefault(true);
        base.addPackage("*");
        base.setEventProcessingMode(EventProcessingOption.STREAM);
        final KieSessionModel kieSessionModel = base.newKieSessionModel(kSessionName).setDefault(true)
                .setType(KieSessionModel.KieSessionType.STATEFUL);
        kieSessionModel.setClockType(ClockTypeOption.get(ClockType.PSEUDO_CLOCK.getId()));

        LOG.debug("kmodule.xml: {}", module.toXML());
        kfs.writeKModuleXML(module.toXML());
        kfs.generateAndWritePomXML(id);

        final List<File> rulesFiles;
        try {
            rulesFiles = getRulesFiles();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LOG.info("Using rules files: {}", rulesFiles);
        for (File file : rulesFiles) {
            kfs.write("src/main/resources/" + file.getName(), ResourceFactory.newFileResource(file));
        }

        // Validate
        final KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll(); // kieModule is automatically deployed to KieRepository if successfully built.
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Build Errors:\n" + kb.getResults().toString());
        }

        LOG.info("Successfully built KIE module with ID: {}.", id);
        return id;
    }

    private List<File> getRulesFiles() throws IOException, URISyntaxException {
        final Path droolsRulesRoot = Paths.get(ClassLoader.getSystemResource("opennms").toURI());
        if (!droolsRulesRoot.toFile().isDirectory()) {
            throw new IllegalStateException("Expected to find Drools rules for alarmd in '" + droolsRulesRoot
                    + "' but the path is not a directory! Aborting.");
        }
        return Files.find(droolsRulesRoot, 3, (path, attrs) -> attrs.isRegularFile()
                        && path.toString().endsWith(".drl"))
                .map(Path::toFile)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    private static ReleaseId generateReleaseId() {
        final KieServices ks = KieServices.Factory.get();
        final String moduleName = UUID.randomUUID().toString();
        return ks.newReleaseId(DroolsContext.class.getPackage().getName(), moduleName, "1.0.0");
    }

    public SessionPseudoClock getClock() {
        return clock;
    }

    public void tick() {
        kieSession.fireAllRules();
    }

    public void insertOrUpdateAlarm(Alarm alarm) {
        handleNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void clearAlarm(Alarm alarm, Date now) {
        LOG.error("Clearing alarm: {}", alarm);

        Alarm newAlarm = alarm.clone();
        newAlarm.setSeverity(2);
        newAlarm.setTimestamp(now.getTime());

        handleNewOrUpdatedAlarm(newAlarm);
    }

    @Override
    public void unclearAlarm(Alarm alarm, Date now) {
        LOG.error("Unclearing alarm: {}", alarm);

        Alarm newAlarm = alarm.clone();
        newAlarm.setSeverity(newAlarm.getLastEventSeverity());
        newAlarm.setTimestamp(now.getTime());

        handleNewOrUpdatedAlarm(newAlarm);
    }

    @Override
    public void setSeverity(Alarm alarm, long severity, Date now) {
        LOG.error("Setting severity to {} on alarm: {}", severity, alarm);

        Alarm newAlarm = alarm.clone();
        newAlarm.setSeverity(severity);
        newAlarm.setTimestamp(now.getTime());

        handleNewOrUpdatedAlarm(newAlarm);
    }

    private void handleNewOrUpdatedAlarm(Alarm alarm) {
        final AlarmAndFact alarmAndFact = alarmsById.get(alarm.getId());
        if (alarmAndFact == null) {
            LOG.error("Inserting alarm into session: {}", alarm);
            final FactHandle fact = kieSession.insert(alarm);
            alarmsById.put(alarm.getId(), new AlarmAndFact(alarm, fact));
        } else {
            // Updating the fact doesn't always give us to expected result, so we resort to deleting it
            // and adding it again instead
            LOG.error("Deleting alarm from session (for re-insertion): {}", alarm);
            kieSession.delete(alarmAndFact.getFact());
            // Reinsert
            LOG.error("Re-inserting alarm into session: {}", alarm);
            final FactHandle fact = kieSession.insert(alarm);
            alarmsById.put(alarm.getId(), new AlarmAndFact(alarm, fact));
        }
    }

    @Override
    public void deleteAlarm(Alarm alarm) {
        final AlarmAndFact alarmAndFact = alarmsById.remove(alarm.getId());
        submitOrRun(kieSession -> {
            LOG.error("Deleting alarm from session: {}", alarmAndFact.getAlarm());
            kieSession.delete(alarmAndFact.getFact());
        });
    }

    public Alarm getAlarmById(Long id) {
        final AlarmAndFact alarmAndFact =  alarmsById.get(id);
        return alarmAndFact != null ? alarmAndFact.getAlarm() : null;
    }

    private void submitOrRun(KieSession.AtomicAction atomicAction) {
        kieSession.submit(atomicAction);
    }
}
