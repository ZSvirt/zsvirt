package org.zstack.zwatch.datatype;

import org.zstack.core.progress.TaskTracker;

import java.util.*;

import static java.util.Arrays.asList;

public class EventFamily {
    private String namespace;
    private String name;
    private String description;
    private List<String> labelNames = new ArrayList<>();
    private EmergencyLevel emergencyLevel;
    private boolean adminOnly;
    private Map<Class, List<EventCollector>> collectors = new HashMap<>();
    private Map<Class, List<MessageBarrier>> messageBarriers = new HashMap<>();
    private Map<String, List<EventBarrier>> eventBarriers = new HashMap<>();
    private Map<String, List<EventThrottleBarrierCleaner>> barrierCleaners = new HashMap<>();
    private Map<String, List<EventCollector3>> collectorsOnCanonicalEvents = new HashMap<>();
    private Map<String, List<EventCollector4>> taskCollectors = new HashMap<>();
    private EventResourceIdGetter resourceIdGetter;
    private RecoverResourceIdGetter recoverResourceIdGetter;

    public enum EmergencyLevel {
        Emergent,
        Important,
        Normal,
        Recovery
    }

    public static class Event {
        private String resourceId;
        private List<String> labelValues = new ArrayList<>();
        private String error;

        public String getError() {
            return error;
        }

        public Event setError(String error) {
            this.error = error;
            return this;
        }

        public Event() {
        }

        public Event(String resourceId) {
            this(resourceId, (String[])null);
        }


        public Event(String resourceId, String...values) {
            this.resourceId = resourceId;
            if (values != null) {
                labelValues = asList(values);
            }
        }

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public List<String> getLabelValues() {
            return labelValues;
        }

        public void setLabelValues(List<String> labelValues) {
            this.labelValues = labelValues;
        }
    }

    public interface EventCollector<T, K> {
        Event collect(T request, K response);
    }

    public interface MessageBarrier<T> {
        String getIdentifier(T msg);
    }

    public interface EventBarrier<T> {
        String getIdentifier(T canonicalEvent);
    }

    public interface EventResourceIdGetter<T> {
        String getResourceId(T msg);
    }

    public interface RecoverResourceIdGetter<T> {
        String getResourceId(T data);
    }

    public interface EventCollector2<T, K> extends EventCollector<T, K> {
        void beforeMessageSent(T request);
    }

    public interface EventCollector3<T> {
        Event collect(T canonicalEvent);
    }

    public interface EventThrottleBarrierCleaner<T> {
        String getIdentifier(T canonicalEvent);
    }

    public interface EventCollector4 {
        Event collect(TaskTracker.Task task);
    }

    public Map<String, List<EventCollector4>> getTaskCollectors() {
        return taskCollectors;
    }

    public void setTaskCollectors(Map<String, List<EventCollector4>> taskCollectors) {
        this.taskCollectors = taskCollectors;
    }

    public EventResourceIdGetter getResourceIdGetter() {
        return resourceIdGetter;
    }

    public RecoverResourceIdGetter getRecoverResourceIdGetter() {
        return recoverResourceIdGetter;
    }

    public EventFamily onMessage(Class apiClz, EventCollector collector) {
        List<EventCollector> cs = collectors.computeIfAbsent(apiClz, k->new ArrayList<>());
        cs.add(collector);
        return this;
    }

    public EventFamily onMessageBarrier(Class apiClz, MessageBarrier throttleBarrier) {
        List<MessageBarrier> tbs = messageBarriers.computeIfAbsent(apiClz, k->new ArrayList<>());
        tbs.add(throttleBarrier);
        return this;
    }

    public EventFamily onEventBarrier(String eventPath, EventBarrier throttleBarrier) {
        List<EventBarrier> tbs = eventBarriers.computeIfAbsent(eventPath, k->new ArrayList<>());
        tbs.add(throttleBarrier);
        return this;
    }

    public EventFamily onTaskTracker(String taskName, EventCollector4 collector) {
        List<EventCollector4> cs = taskCollectors.computeIfAbsent(taskName, k->new ArrayList<>());
        cs.add(collector);
        return this;
    }

    public EventFamily onMessage(Class apiClz, EventCollector2 collector) {
        onMessage(apiClz, (EventCollector)collector);
        return this;
    }

    public EventFamily onEventBarriersCleaner(String eventPath, EventThrottleBarrierCleaner barrierCleaner) {
        List<EventThrottleBarrierCleaner> bcs = barrierCleaners.computeIfAbsent(eventPath, k->new ArrayList<>());
        bcs.add(barrierCleaner);
        return this;
    }

    public EventFamily onCanonicalEvent(String eventPath, EventCollector3 collector) {
        List<EventCollector3> cs = collectorsOnCanonicalEvents.computeIfAbsent(eventPath, k->new ArrayList<>());
        cs.add(collector);
        return this;
    }

    public EventFamily(String name, Collection<EventFamily> families, Enum...labels) {
        this(name, false, EmergencyLevel.Normal, families, labels);
    }

    public EventFamily(String name, boolean adminOnly, Collection<EventFamily> families, Enum...labels) {
        this(name, adminOnly, EmergencyLevel.Normal, families, labels);
    }

    public EventFamily(String name, boolean adminOnly, EmergencyLevel level, Collection<EventFamily> families, Enum...labels) {
        this.name = name;
        this.adminOnly = adminOnly;
        this.emergencyLevel = level;
        for (Enum e : labels) {
            labelNames.add(e.toString());
        }
        families.add(this);
    }

    public EventFamily onErrorReturnResourceId(EventResourceIdGetter getter) {
        resourceIdGetter = getter;
        return this;
    }

    public EventFamily onRecoverReturnResourceId(RecoverResourceIdGetter getter) {
        recoverResourceIdGetter = getter;
        return this;
    }

    public Map<String, List<EventCollector3>> getCollectorsOnCanonicalEvents() {
        return collectorsOnCanonicalEvents;
    }

    public void setCollectorsOnCanonicalEvents(Map<String, List<EventCollector3>> collectorsOnCanonicalEvents) {
        this.collectorsOnCanonicalEvents = collectorsOnCanonicalEvents;
    }

    public Map<Class, List<EventCollector>> getCollectors() {
        return collectors;
    }

    public List<EventCollector> getCollectorsByMessageClass(Class clz) {
        return collectors.get(clz);
    }

    public List<MessageBarrier> getMessageThrottleBarriersByMessageClass(Class clz) {
        return messageBarriers.get(clz);
    }

    public List<EventBarrier> getEventBarriersByEventPath(String eventPath) {
        return eventBarriers.get(eventPath);
    }

    public Map<String, List<EventThrottleBarrierCleaner>> getBarrierCleaners() {
        return barrierCleaners;
    }

    public void setCollectors(Map<Class, List<EventCollector>> collectors) {
        this.collectors = collectors;
    }

    public EmergencyLevel getEmergencyLevel() {
        return emergencyLevel;
    }

    public EventFamily setEmergencyLevel(EmergencyLevel emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public EventFamily setNamespace(String namespace) {
        this.namespace = namespace;
        this.description = ZWatchI18n.generateDescriptionFromName(namespace, getName());
        return this;
    }

    public String getName() {
        return name;
    }

    public EventFamily setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getLabelNames() {
        return labelNames;
    }

    public EventFamily setLabelNames(List<String> labelNames) {
        this.labelNames = labelNames;
        return this;
    }

    public boolean isAdminOnly() {
        return adminOnly;
    }

    public void setAdminOnly(boolean adminOnly) {
        this.adminOnly = adminOnly;
    }

    public String getDescription() {
        return description;
    }
}
