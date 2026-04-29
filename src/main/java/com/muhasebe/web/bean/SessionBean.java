package com.muhasebe.web.bean;

import com.muhasebe.user.domain.UserEntity;
import com.muhasebe.user.domain.UserRole;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * Session boyunca giris yapmis kullaniciyi tutar.
 * XHTML'lerden #{sessionBean.currentUser} ile erisilir.
 */
@Named
@SessionScoped
public class SessionBean implements Serializable {

    private UserEntity currentUser;

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }

    public boolean isKullanici() {
        return currentUser != null && currentUser.getRole() == UserRole.KULLANICI;
    }

    public UserEntity getCurrentUser() { return currentUser; }
    public void setCurrentUser(UserEntity currentUser) { this.currentUser = currentUser; }
}