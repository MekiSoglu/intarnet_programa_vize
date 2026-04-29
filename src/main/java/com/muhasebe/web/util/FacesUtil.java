package com.muhasebe.web.util;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

import java.io.IOException;

// hata mesajlarını bildirimleri otomatik yönetir
public final class FacesUtil {

    private FacesUtil() {}

    public static void info(String summary) {
        addMessage(FacesMessage.SEVERITY_INFO, summary, null);
    }

    public static void info(String summary, String detail) {
        addMessage(FacesMessage.SEVERITY_INFO, summary, detail);
    }

    public static void warn(String summary) {
        addMessage(FacesMessage.SEVERITY_WARN, summary, null);
    }

    public static void error(String summary) {
        addMessage(FacesMessage.SEVERITY_ERROR, summary, null);
    }

    public static void error(String summary, String detail) {
        addMessage(FacesMessage.SEVERITY_ERROR, summary, detail);
    }

    public static void error(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) msg = t.getClass().getSimpleName();
        addMessage(FacesMessage.SEVERITY_ERROR, "Hata", msg);
    }

    private static void addMessage(FacesMessage.Severity sev, String summary, String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ctx.addMessage(null, new FacesMessage(sev, summary, detail));
        }
    }

    public static String redirect(String outcome) {
        return outcome + "?faces-redirect=true";
    }

    public static String getRequestParam(String name) {
        return FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap().get(name);
    }
}
