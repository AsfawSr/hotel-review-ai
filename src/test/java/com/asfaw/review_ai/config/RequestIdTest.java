package com.asfaw.review_ai.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdTest {

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void reusesValidClientIdAndExposesItInMdcDuringTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        new RequestIdFilter().doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seen.set(MDC.get(RequestIdFilter.MDC_KEY));
            }
        });

        assertThat(seen.get()).isEqualTo("abc-123");
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("abc-123");
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesMalformedIds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "bad id\nFAKE LOG LINE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestIdFilter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestIdFilter.HEADER)).matches("[0-9a-f-]{36}");
    }

    @Test
    void decoratorPropagatesMdcToWorkerThreads() throws Exception {
        MDC.put(RequestIdFilter.MDC_KEY, "req-42");
        Runnable decorated = new MdcTaskDecorator().decorate(() -> assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isEqualTo("req-42"));

        CompletableFuture.runAsync(decorated).get();
    }
}
