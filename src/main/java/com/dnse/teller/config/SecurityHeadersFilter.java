package com.dnse.teller.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityHeadersFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpServletResponse) {
            httpServletResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpServletResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
            httpServletResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            httpServletResponse.setHeader("Pragma", "no-cache");
        }
        chain.doFilter(request, response);
    }
}
