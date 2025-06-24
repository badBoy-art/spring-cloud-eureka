package com.example.filter;

import com.github.phantomthief.scope.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * @author: badBoy
 * @create: 2024-04-12 10:30
 * @Description:
 */
@Order(0)
@Service
@ConditionalOnClass(name = {"javax.servlet.Filter"})
public class MyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        Scope scope = Scope.getCurrentScope();
        try {
            if (Objects.isNull(scope)) {
                Scope.beginScope();
            }
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            if (Objects.isNull(scope)) {
                Scope.endScope();
            }
        }
    }

}
