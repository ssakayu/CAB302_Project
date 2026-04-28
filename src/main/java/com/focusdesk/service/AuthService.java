package com.focusdesk.service;

import com.focusdesk.dao.PreferenceDAO;
import com.focusdesk.dao.UserDAO;
import com.focusdesk.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final PreferenceDAO prefDAO = new PreferenceDAO();

    public User signUp(String username, String email, String plainPassword) throws Exception {
        username = username == null ? "" : username.trim();
        email = email == null ? "" : email.trim().toLowerCase();

        if (username.isBlank())
            throw new IllegalArgumentException("Username required");
        if (username.length() < 3)
            throw new IllegalArgumentException("Username must be at least 3 characters");
        if (email.isBlank())
            throw new IllegalArgumentException("Email required");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Invalid email address");
        if (plainPassword == null || plainPassword.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters");

        if (userDAO.findByUsername(username) != null)
            throw new IllegalArgumentException("Username already taken");
        if (userDAO.findByEmail(email) != null)
            throw new IllegalArgumentException("Email already registered");

        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
        User created = userDAO.create(username, email, hash);

        // create default preferences row
        prefDAO.ensureDefaultRow(created.getId());

        return created;
    }

    public User login(String email, String plainPassword) throws Exception {
        email = email == null ? "" : email.trim().toLowerCase();
        User user = userDAO.findByEmail(email);
        if (user == null)
            throw new IllegalArgumentException("Invalid email or password");

        boolean ok = BCrypt.checkpw(plainPassword, user.getPasswordHash());
        if (!ok)
            throw new IllegalArgumentException("Invalid email or password");

        return user;
    }
}