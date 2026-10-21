package com.fielddispatch.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum IncidentStatus {
    open,
    triaged,
    assigned,
    resolved,
    closed;

    private static final Map<IncidentStatus, Set<IncidentStatus>> TRANSITIONS = new EnumMap<>(IncidentStatus.class);

    static {
        TRANSITIONS.put(open, EnumSet.of(triaged));
        TRANSITIONS.put(triaged, EnumSet.of(assigned, resolved));
        TRANSITIONS.put(assigned, EnumSet.of(resolved));
        TRANSITIONS.put(resolved, EnumSet.of(closed, triaged));
        TRANSITIONS.put(closed, EnumSet.noneOf(IncidentStatus.class));
    }

    public boolean canTransitionTo(IncidentStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
