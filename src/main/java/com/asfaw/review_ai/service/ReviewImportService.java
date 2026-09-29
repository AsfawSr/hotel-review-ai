package com.asfaw.review_ai.service;

import com.asfaw.review_ai.web.dto.ReviewSubmissionRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Imports reviews from CSV with a header row: guestName, reviewText, rating (optional; aliases accepted).
 * Valid rows are saved and queued for analysis; invalid rows are reported with their line number.
 */
@Service
@RequiredArgsConstructor
public class ReviewImportService {

    public static final int MAX_ROWS = 500;

    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("guestname", "guestName"), Map.entry("guest", "guestName"),
            Map.entry("name", "guestName"), Map.entry("reviewer", "guestName"),
            Map.entry("reviewtext", "reviewText"), Map.entry("review", "reviewText"),
            Map.entry("text", "reviewText"), Map.entry("comment", "reviewText"),
            Map.entry("rating", "rating"), Map.entry("stars", "rating"), Map.entry("score", "rating"));

    private final ReviewService reviewService;
    private final Validator validator;

    public record RowError(long line, String message) {
    }

    public record ImportResult(int imported, int skipped, List<RowError> errors) {
    }

    public ImportResult importCsv(Reader reader) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true).get();
        try (CSVParser parser = CSVParser.parse(reader, format)) {
            Map<String, String> columns = resolveColumns(parser.getHeaderNames());
            int imported = 0;
            List<RowError> errors = new ArrayList<>();
            // Physical line tracking: quoted fields can span several lines, so record numbers are not line numbers.
            long previousEndLine = parser.getCurrentLineNumber();
            for (CSVRecord record : parser) {
                if (record.getRecordNumber() > MAX_ROWS) {
                    throw new ResponseStatusException(BAD_REQUEST, "At most " + MAX_ROWS + " rows can be imported at once");
                }
                long line = previousEndLine + 1;
                previousEndLine = parser.getCurrentLineNumber();
                try {
                    ReviewSubmissionRequest request = toRequest(record, columns);
                    Set<ConstraintViolation<ReviewSubmissionRequest>> violations = validator.validate(request);
                    if (!violations.isEmpty()) {
                        errors.add(new RowError(line, describe(violations)));
                        continue;
                    }
                    reviewService.createReview(request);
                    imported++;
                } catch (IllegalArgumentException ex) {
                    errors.add(new RowError(line, ex.getMessage()));
                }
            }
            return new ImportResult(imported, errors.size(), errors);
        } catch (IOException | java.io.UncheckedIOException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "The file is not valid CSV: " + ex.getMessage());
        }
    }

    private static Map<String, String> resolveColumns(List<String> headers) {
        Map<String, String> columns = new TreeMap<>();
        for (String header : headers) {
            String canonical = HEADER_ALIASES.get(header.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", ""));
            if (canonical != null) {
                columns.putIfAbsent(canonical, header);
            }
        }
        if (!columns.containsKey("guestName") || !columns.containsKey("reviewText")) {
            throw new ResponseStatusException(BAD_REQUEST, "CSV header must include guestName and reviewText columns");
        }
        return columns;
    }

    private static ReviewSubmissionRequest toRequest(CSVRecord record, Map<String, String> columns) {
        String guestName = value(record, columns.get("guestName"));
        String reviewText = value(record, columns.get("reviewText"));
        String rawRating = columns.containsKey("rating") ? value(record, columns.get("rating")) : "";
        Integer rating = null;
        if (!rawRating.isEmpty()) {
            try {
                rating = (int) Math.round(Double.parseDouble(rawRating));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("rating: not a number");
            }
        }
        return new ReviewSubmissionRequest(guestName, reviewText, rating);
    }

    private static String value(CSVRecord record, String column) {
        return record.isMapped(column) && record.isSet(column) ? record.get(column) : "";
    }

    private static String describe(Set<ConstraintViolation<ReviewSubmissionRequest>> violations) {
        return violations.stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("invalid row");
    }
}
