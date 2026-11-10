package dev.rotadev.tracking.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.identity.UserTimeZones;
import dev.rotadev.tracking.TrackingReader;
import dev.rotadev.tracking.domain.ReviewHabit;
import dev.rotadev.tracking.domain.StudySession;

@Service
class TrackingQueries implements TrackingReader {

    private final TrackingStore store;
    private final UserTimeZones timeZones;
    private final Clock clock;

    TrackingQueries(TrackingStore store, UserTimeZones timeZones, Clock clock) {
        this.store = store;
        this.timeZones = timeZones;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public int minutesBetween(UUID userId, LocalDate from, LocalDate to) {
        return store.sessions(userId, from, to).stream().mapToInt(StudySession::minutes).sum();
    }

    @Override
    @Transactional(readOnly = true)
    public int completeReviewDaysBetween(UUID userId, LocalDate from, LocalDate to) {
        return (int) store.habits(userId, from, to).stream().filter(ReviewHabit::complete).count();
    }

    @Override
    @Transactional(readOnly = true)
    public int milestonesAchieved(UUID userId) {
        return store.achievements(userId).size();
    }

    @Override
    public LocalDate today(UUID userId) {
        return LocalDate.now(clock.withZone(timeZones.of(userId)));
    }
}
