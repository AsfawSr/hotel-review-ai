package com.asfaw.review_ai.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewModelAttributes {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        if (request == null) {
            return "/";
        }

        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return "/";
        }

        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        if (uri.isBlank()) {
            return "/";
        }

        return uri;
    }
}

