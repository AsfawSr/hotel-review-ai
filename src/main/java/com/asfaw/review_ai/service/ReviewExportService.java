package com.asfaw.review_ai.service;

import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.entity.ReviewAnalysis;
import com.asfaw.review_ai.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.stream.Collectors;

/**
 * Writes filtered reviews with their analysis as CSV (newest first, capped at {@link #MAX_ROWS}).
 */
@Service
@RequiredArgsConstructor
public class ReviewExportService {

    public static final int MAX_ROWS = 5_000;

    static final String[] HEADERS = {
            "id", "submittedAt", "guestName", "rating", "reviewText", "analysisStatus", "sentiment", "sentimentScore",
            "mainTopic", "topics", "language", "analysisSource", "managerResponse"
    };

    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;

    @Transactional(readOnly = true)
    public int export(ReviewService.ReviewFilters filters, Writer writer) {
        var page = reviewRepository.findAll(reviewService.buildSpecification(filters),
                PageRequest.of(0, MAX_ROWS, Sort.by(Sort.Direction.DESC, "submittedAt")));
        try {
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).get());
            for (Review review : page) {
                ReviewAnalysis a = review.getAnalysis();
                printer.printRecord(
                        review.getId(),
                        review.getSubmittedAt(),
                        safe(review.getGuestName()),
                        review.getRating(),
                        safe(review.getReviewText()),
                        review.getAnalysisStatus(),
                        a == null ? null : a.getSentiment(),
                        a == null ? null : a.getSentimentScore(),
                        a == null ? null : a.getMainTopic(),
                        a == null ? null : a.getTopics().stream().map(Enum::name).collect(Collectors.joining("|")),
                        a == null ? null : a.getLanguage(),
                        a == null ? null : a.getSource(),
                        a == null ? null : safe(a.getManagerResponse()));
            }
            printer.flush();
            return page.getNumberOfElements();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Neutralizes spreadsheet formula injection (OWASP "CSV injection"): guest-controlled text starting with
     * =, +, -, @, tab or carriage return is prefixed with an apostrophe so Excel/Sheets treat it as text.
     */
    static String safe(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r'
                ? "'" + value
                : value;
    }
}
