package com.muhasebe.web.bean;

import com.muhasebe.user.domain.UserEntity;
import com.muhasebe.user.service.UserService;
import com.muhasebe.web.util.FacesUtil;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;

@Named
@ViewScoped
public class LoginBean implements Serializable {

    @Inject
    private UserService userService;

    @Inject
    private SessionBean sessionBean;

    private String username;
    private String password;

    public String login() {
        UserEntity user = userService.authenticate(username, password);
        if (user == null) {
            FacesUtil.error("Kullanici adi veya sifre hatali");
            password = null;
            return null;
        }

        sessionBean.setCurrentUser(user);
        return "/index.xhtml?faces-redirect=true";
    }

    public String logout() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();
        req.getSession().invalidate();
        return "/login.xhtml?faces-redirect=true";
    }

    // Getters & Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}