package com.muhasebe.web.bean;

import com.muhasebe.user.domain.UserEntity;
import com.muhasebe.user.domain.UserRole;
import com.muhasebe.user.service.UserService;
import com.muhasebe.web.util.FacesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class UserManagementBean implements Serializable {

    @Inject
    private UserService userService;

    @Inject
    private SessionBean sessionBean;

    private List<UserEntity> users = new ArrayList<>();

    // Yeni kullanici formu
    private String newUsername;
    private String newPassword;
    private String newFullName;
    private UserRole newRole = UserRole.KULLANICI;

    // Sifre degistirme dialog'u
    private Long passwordChangeUserId;
    private String passwordChangeUsername;
    private String passwordChangeNewPassword;

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        try {
            users = userService.listAll();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void createUser() {
        try {
            userService.createUser(newUsername, newPassword, newRole, newFullName);
            FacesUtil.info("Kullanici olusturuldu: " + newUsername);
            // Form temizle
            newUsername = null;
            newPassword = null;
            newFullName = null;
            newRole = UserRole.KULLANICI;
            refresh();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void deleteUser(UserEntity user) {
        // Admin kendi kendini silemez
        if (sessionBean.getCurrentUser() != null
                && sessionBean.getCurrentUser().getId().equals(user.getId())) {
            FacesUtil.warn("Kendi hesabinizi silemezsiniz");
            return;
        }
        try {
            userService.deleteUser(user.getId());
            FacesUtil.info("Kullanici silindi: " + user.getUsername());
            refresh();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public void changeRole(UserEntity user, UserRole newRole) {
        // Admin kendi rolunu dusuremez (sistem kilitlenmesin)
        if (sessionBean.getCurrentUser() != null
                && sessionBean.getCurrentUser().getId().equals(user.getId())
                && newRole != UserRole.ADMIN) {
            FacesUtil.warn("Kendi rolunuzu dusuremezsiniz");
            return;
        }
        try {
            userService.updateRole(user.getId(), newRole);
            FacesUtil.info("Rol guncellendi: " + user.getUsername() + " -> " + newRole);
            refresh();
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    /** Sifre degistirme dialog'unu hazirla. */
    public void preparePasswordChange(UserEntity user) {
        passwordChangeUserId = user.getId();
        passwordChangeUsername = user.getUsername();
        passwordChangeNewPassword = null;
    }

    public void confirmPasswordChange() {
        try {
            userService.updatePassword(passwordChangeUserId, passwordChangeNewPassword);
            FacesUtil.info("Sifre guncellendi: " + passwordChangeUsername);
            passwordChangeUserId = null;
            passwordChangeUsername = null;
            passwordChangeNewPassword = null;
        } catch (Exception e) {
            FacesUtil.error(e);
        }
    }

    public boolean isCurrentUser(UserEntity user) {
        return sessionBean.getCurrentUser() != null
                && sessionBean.getCurrentUser().getId().equals(user.getId());
    }

    public UserRole[] getAllRoles() {
        return UserRole.values();
    }

    // Getters & Setters
    public List<UserEntity> getUsers() { return users; }

    public String getNewUsername() { return newUsername; }
    public void setNewUsername(String newUsername) { this.newUsername = newUsername; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getNewFullName() { return newFullName; }
    public void setNewFullName(String newFullName) { this.newFullName = newFullName; }

    public UserRole getNewRole() { return newRole; }
    public void setNewRole(UserRole newRole) { this.newRole = newRole; }

    public Long getPasswordChangeUserId() { return passwordChangeUserId; }
    public String getPasswordChangeUsername() { return passwordChangeUsername; }
    public String getPasswordChangeNewPassword() { return passwordChangeNewPassword; }
    public void setPasswordChangeNewPassword(String passwordChangeNewPassword) {
        this.passwordChangeNewPassword = passwordChangeNewPassword;
    }
}