package com.fielddispatch.workflow;

import com.fielddispatch.domain.JobStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobStatusTest {

    @Test
    void unassignedCanMoveToAssignedOrCancelled() {
        assertTrue(JobStatus.unassigned.canTransitionTo(JobStatus.assigned));
        assertTrue(JobStatus.unassigned.canTransitionTo(JobStatus.cancelled));
        assertFalse(JobStatus.unassigned.canTransitionTo(JobStatus.completed));
    }

    @Test
    void completedIsTerminal() {
        for (JobStatus target : JobStatus.values()) {
            assertFalse(JobStatus.completed.canTransitionTo(target), "completed should not transition to " + target);
        }
    }

    @Test
    void inProgressCanCompleteOrFail() {
        assertTrue(JobStatus.in_progress.canTransitionTo(JobStatus.completed));
        assertTrue(JobStatus.in_progress.canTransitionTo(JobStatus.failed));
        assertFalse(JobStatus.in_progress.canTransitionTo(JobStatus.unassigned));
    }

    @Test
    void everyStatusIsReachable() {
        // Every status named in the spec must be reachable via some transition,
        // except 'unassigned' which is the initial/reset state.
        for (JobStatus target : JobStatus.values()) {
            if (target == JobStatus.unassigned) continue;
            boolean reachable = false;
            for (JobStatus source : JobStatus.values()) {
                if (source.canTransitionTo(target)) {
                    reachable = true;
                    break;
                }
            }
            assertTrue(reachable, target + " must be reachable through some transition");
        }
    }
}
