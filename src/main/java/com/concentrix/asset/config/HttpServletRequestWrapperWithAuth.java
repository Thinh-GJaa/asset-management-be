package com.concentrix.asset.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class HttpServletRequestWrapperWithAuth extends HttpServletRequestWrapper {
    private final String authHeader;

    public HttpServletRequestWrapperWithAuth(HttpServletRequest request, String authHeader) {
        super(request);
        this.authHeader = authHeader;
    }

    @Override
    public String getHeader(String name) {
        if ("Authorization".equalsIgnoreCase(name)) {
            return authHeader;
        }
        return super.getHeader(name);
    }
}
