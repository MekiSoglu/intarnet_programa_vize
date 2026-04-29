package com.muhasebe.web.filter;

import com.muhasebe.web.bean.SessionBean;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

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
        if (!loggedIn) {
            redirectToLogin(request, response);
            return;
        }

        // ADMIN-only sayfalar - URL bazli ekstra koruma
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isAdminOnlyPath(path) && !sessionBean.isAdmin()) {
            redirectToHome(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isAdminOnlyPath(String path) {
        return path.startsWith("/pages/users/")
                || path.startsWith("/pages/categories/list")
                || path.startsWith("/pages/categories/details")
                || path.startsWith("/pages/tables/create")
                || path.startsWith("/pages/views/create")
                || path.startsWith("/pages/procedures/create")
                || path.startsWith("/pages/logs/");
    }

    private void redirectToLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String url = req.getContextPath() + "/login.xhtml";
        if (isAjaxRequest(req)) {
            resp.setContentType("text/xml");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(
                    "<?xml version='1.0' encoding='UTF-8'?>" +
                            "<partial-response><redirect url=\"" + url + "\"/></partial-response>");
        } else {
            resp.sendRedirect(url);
        }
    }

    private void redirectToHome(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String url = req.getContextPath() + "/index.xhtml";
        resp.sendRedirect(url);
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "partial/ajax".equals(request.getHeader("Faces-Request"));
    }
}