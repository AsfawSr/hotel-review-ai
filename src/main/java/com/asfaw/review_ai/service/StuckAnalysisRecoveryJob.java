package com.asfaw.review_ai.service;

import com.asfaw.review_ai.config.AnalysisProperties;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Re-queues analyses lost to restarts, crashes or a full executor queue, since @Async jobs live only in memory.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StuckAnalysisRecoveryJob {

    private final ReviewRepository reviewRepository;
    private final ReviewAnalysisProcessingService processingService;
    private final AnalysisProperties properties;
    private final AnalysisMetrics metrics;

    @Scheduled(fixedDelayString = "${app.analysis.recovery-interval:PT1M}",
            initialDelayString = "${app.analysis.recovery-interval:PT1M}")
    public void recover() {
        Instant now = Instant.now();

        int requeued = reviewRepository.transitionStaleStatus(
                AnalysisStatus.PROCESSING, AnalysisStatus.PENDING, now.minus(properties.processingTimeout()), now);
        if (requeued > 0) {
            log.warn("Re-queued {} review(s) stuck in PROCESSING for more than {}", requeued, properties.processingTimeout());
            metrics.recovered("processing", requeued);
        }

        // Rows re-queued above are stamped with `now`, so they are dispatched on the next run after the grace period.
        List<Long> pendingIds = reviewRepository.findIdsByStatusUpdatedBefore(
                AnalysisStatus.PENDING, now.minus(properties.pendingGrace()), PageRequest.of(0, properties.recoveryBatchSize()));
        if (!pendingIds.isEmpty()) {
            log.info("Dispatching {} pending review(s) for analysis", pendingIds.size());
            metrics.recovered("pending", pendingIds.size());
            for (Long id : pendingIds) {
                try {
                    processingService.processReviewAsync(id);
                } catch (TaskRejectedException ex) {
                    log.warn("Analysis queue full; remaining pending reviews will be retried next run");
                    break;
                }
            }
        }
    }
}
