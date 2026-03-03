package com.weatherwise.controllers;

import com.weatherwise.services.UserService;
import io.javalin.Javalin;

import java.util.List;
import java.util.Map;

public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    public void registerRoutes(Javalin app) {

        // REGISTER
        app.post("/api/v1/auth/register", ctx -> {

            RegisterRequest body = ctx.bodyAsClass(RegisterRequest.class);

            boolean ok = userService.register(
                    body.username,
                    body.password,
                    body.preferredCategories
            );

            if (!ok) {
                ctx.status(400).json(Map.of("error", "User exists or invalid input"));
                return;
            }

            ctx.json(Map.of("status", "registered"));
        });

        // LOGIN
        app.post("/api/v1/auth/login", ctx -> {

            LoginRequest body = ctx.bodyAsClass(LoginRequest.class);

            UserService.User user = userService.login(body.username, body.password);

            if (user == null) {
                ctx.status(401).json(Map.of("error", "Invalid login"));
                return;
            }

            ctx.json(Map.of(
                    "token", user.token,
                    "preferredCategories", user.preferredCategories
            ));
        });

        // ME
        app.get("/api/v1/auth/me", ctx -> {

            String token = getToken(ctx);

            if (token == null) {
                ctx.status(401).json(Map.of("error", "Missing token"));
                return;
            }

            UserService.User user = userService.getByToken(token);

            if (user == null) {
                ctx.status(401).json(Map.of("error", "Invalid token"));
                return;
            }

            ctx.json(Map.of(
                    "username", user.username,
                    "preferredCategories", user.preferredCategories
            ));
        });

        // SAVE PREFERENCES
        app.put("/api/v1/auth/preferences", ctx -> {

            String token = getToken(ctx);
            if (token == null) {
                ctx.status(401).json(Map.of("error", "Missing token"));
                return;
            }

            PreferencesRequest body = ctx.bodyAsClass(PreferencesRequest.class);

            boolean ok = userService.savePreferences(token, body.preferredCategories);

            if (!ok) {
                ctx.status(401).json(Map.of("error", "Invalid token"));
                return;
            }

            ctx.json(Map.of("status", "saved"));
        });
    }

    // ================= Helper =================

    private String getToken(io.javalin.http.Context ctx) {
        String auth = ctx.header("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return auth.substring(7);
    }

    // ================= Request-klasser =================

    public static class RegisterRequest {
        public String username;
        public String password;
        public List<String> preferredCategories;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class PreferencesRequest {
        public List<String> preferredCategories;
    }
}