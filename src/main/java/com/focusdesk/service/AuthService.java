package com.focusdesk.service;

import com.focusdesk.dao.PreferenceDAO;
import com.focusdesk.dao.UserDAO;
import com.focusdesk.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final PreferenceDAO prefDAO = new PreferenceDAO();

    public User signUp(String username, String email, String plainPassword) throws Exception {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username required");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email required");
        if (plainPassword == null || plainPassword.length() < 6) throw new IllegalArgumentException("Password min 6 chars");

        User existing = userDAO.findByEmail(email);
        if (existing != null) throw new IllegalArgumentException("Email already registered");

        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
        User created = userDAO.create(username.trim(), email.trim(), hash);

        // create default preferences row
        prefDAO.ensureDefaultRow(created.getId());

        return created;
    }

    public User login(String email, String plainPassword) throws Exception {
        User user = userDAO.findByEmail(email);
        if (user == null) throw new IllegalArgumentException("Invalid email or password");

        boolean ok = BCrypt.checkpw(plainPassword, user.getPasswordHash());
        if (!ok) throw new IllegalArgumentException("Invalid email or password");

        return user;
    }
}