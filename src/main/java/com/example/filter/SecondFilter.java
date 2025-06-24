package com.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;

/**
 * @author: badBoy
 * @create: 2024-11-18 16:21
 * @Description:
 */
public class SecondFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("SecondFilter executed");
        filterChain.doFilter(request, response);
    }
}
