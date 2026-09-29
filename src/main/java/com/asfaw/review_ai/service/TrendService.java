package com.asfaw.review_ai.service;

import com.asfaw.review_ai.model.enums.ReplyStatus;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrendService {

    public static final int MAX_WEEKS = 52;

    private final ReviewRepository reviewRepository;
    private final Clock clock = Clock.systemUTC();

    public record WeeklyTrend(LocalDate weekStart, long total, long positive, long neutral, long negative, Double averageRating) {
    }

    /** One bucket per ISO week (Monday, UTC) for the last {@code weeks} weeks, oldest first, including empty weeks. */
    @Transactional(readOnly = true)
    public List<WeeklyTrend> weekly(int weeks) {
        int span = Math.clamp(weeks, 1, MAX_WEEKS);
        LocalDate thisWeek = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate firstWeek = thisWeek.minusWeeks(span - 1L);
        Instant since = firstWeek.atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<LocalDate, Accumulator> buckets = new LinkedHashMap<>();
        for (int i = 0; i < span; i++) {
            buckets.put(firstWeek.plusWeeks(i), new Accumulator());
        }
        for (Object[] row : reviewRepository.findTrendRowsSince(since)) {
            LocalDate week = ((Instant) row[0]).atZone(ZoneOffset.UTC).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            Accumulator acc = buckets.get(week);
            if (acc != null) {
                acc.add((Integer) row[1], (Sentiment) row[2]);
            }
        }

        List<WeeklyTrend> result = new ArrayList<>();
        buckets.forEach((week, acc) -> result.add(acc.toTrend(week)));
        return result;
    }

    @Transactional(readOnly = true)
    public long unansweredNegativeCount() {
        return reviewRepository.countNegativeWithoutReplyStatus(Sentiment.NEGATIVE, ReplyStatus.SENT);
    }

    private static final class Accumulator {
        private long total;
        private long positive;
        private long neutral;
        private long negative;
        private long ratingSum;
        private long ratingCount;

        void add(Integer rating, Sentiment sentiment) {
            total++;
            if (rating != null) {
                ratingSum += rating;
                ratingCount++;
            }
            if (sentiment == Sentiment.POSITIVE) positive++;
            else if (sentiment == Sentiment.NEUTRAL) neutral++;
            else if (sentiment == Sentiment.NEGATIVE) negative++;
        }

        WeeklyTrend toTrend(LocalDate week) {
            Double avg = ratingCount == 0 ? null : Math.round(ratingSum * 100.0 / ratingCount) / 100.0;
            return new WeeklyTrend(week, total, positive, neutral, negative, avg);
        }
    }
}
