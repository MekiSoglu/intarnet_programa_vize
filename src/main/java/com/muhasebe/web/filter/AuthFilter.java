package com.muhasebe.web.filter;

import com.muhasebe.web.bean.SessionBean;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * URL koruyucu filter.
 *
 * Korumali path'ler: /pages/* (tum is sayfalari)
 * Acik path'ler:    /login.xhtml, /jakarta.faces.resource/*, /resources/*
 *
 * Login degilse -> /login.xhtml'e yonlendir
 * AJAX istegiyse -> partial-response ile redirect
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/pages/*", "/index.xhtml"})
public class AuthFilter implements Filter {

    @Inject
    private SessionBean sessionBean;

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        boolean loggedIn = sessionBean != null && sessionBean.isLoggedIn();
        if (loggedIn) {
            chain.doFilter(request, response);
            return;
        }

        String loginUrl = request.getContextPath() + "/login.xhtml";

        if (isAjaxRequest(request)) {
            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "<?xml version='1.0' encoding='UTF-8'?>" +
                            "<partial-response><redirect url=\"" + loginUrl + "\"/></partial-response>");
        } else {
            response.sendRedirect(loginUrl);
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String facesRequest = request.getHeader("Faces-Request");
        return "partial/ajax".equals(facesRequest);
    }
}