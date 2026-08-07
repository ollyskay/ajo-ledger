package com.ajosavings.group.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public enum ContributionCycle {
    WEEKLY {
        @Override
        public Instant advance(Instant from) {
            return from.atZone(ZoneOffset.UTC).plusWeeks(1).toInstant();
        }
    },
    MONTHLY {
        @Override
        public Instant advance(Instant from) {
            return from.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
        }
    };

    /** Advances an instant by one period of this cycle, using calendar (not fixed-duration) arithmetic. */
    public abstract Instant advance(Instant from);
}
