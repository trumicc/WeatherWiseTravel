package com.weatherwise.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class UserService {

    public static class User {
        public String username;
        public String password;
        public List<String> preferredCategories = new ArrayList<>();
        public String token;

        public User() {}
    }

    private final ObjectMapper mapper = new ObjectMapper();

    private final Path file = Paths.get("data", "users.json");

    private List<User> users = new ArrayList<>();

    public UserService() {
        loadFromFile();
        System.out.println("Saving users to: " + file.toAbsolutePath());
    }

    public synchronized boolean register(String username, String password, List<String> categories) {
        username = trim(username);
        if (username.isEmpty()) return false;
        if (password == null || password.isBlank()) return false;

        if (findByUsername(username) != null) return false;

        User u = new User();
        u.username = username;
        u.password = password;
        u.preferredCategories = (categories != null) ? new ArrayList<>(categories) : new ArrayList<>();
        u.token = UUID.randomUUID().toString();

        users.add(u);
        saveToFile();
        return true;
    }

    public synchronized User login(String username, String password) {
        username = trim(username);
        if (username.isEmpty()) return null;
        if (password == null) return null;

        User u = findByUsername(username);
        if (u == null) return null;
        if (!Objects.equals(u.password, password)) return null;

        if (u.token == null || u.token.isBlank()) {
            u.token = UUID.randomUUID().toString();
            saveToFile();
        }

        return u;
    }

    public synchronized User getByToken(String token) {
        if (token == null || token.isBlank()) return null;

        for (User u : users) {
            if (u != null && Objects.equals(u.token, token)) return u;
        }
        return null;
    }

    public synchronized boolean savePreferences(String token, List<String> categories) {
        User u = getByToken(token);
        if (u == null) return false;

        u.preferredCategories = (categories != null) ? new ArrayList<>(categories) : new ArrayList<>();
        saveToFile();
        return true;
    }

    private User findByUsername(String username) {
        for (User u : users) {
            if (u != null && Objects.equals(trim(u.username), username)) return u;
        }
        return null;
    }

    private void loadFromFile() {
        try {
            Files.createDirectories(file.getParent());

            if (!Files.exists(file)) {
                Files.writeString(file, "[]", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            String json = Files.readString(file).trim();
            if (json.isEmpty()) json = "[]";

            users = mapper.readValue(json, new TypeReference<List<User>>() {});
            if (users == null) users = new ArrayList<>();

        } catch (Exception e) {
            System.out.println("Could not load users.json: " + e.getMessage());
            users = new ArrayList<>();
        }
    }

    private void saveToFile() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), users);
        } catch (Exception e) {
            System.out.println("Could not save users.json: " + e.getMessage());
        }
    }

    private String trim(String s) {
        return (s == null) ? "" : s.trim();
    }
}