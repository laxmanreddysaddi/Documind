package com.documind.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // 🔥 VERY IMPORTANT (skip ALL APIs)
        if (
            path.startsWith("/api/auth") ||
            path.startsWith("/api/documents") ||
            path.startsWith("/api/chat") ||
            request.getMethod().equals("OPTIONS")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        // ❌ DO NOTHING (skip auth completely for now)
        filterChain.doFilter(request, response);
    }
}