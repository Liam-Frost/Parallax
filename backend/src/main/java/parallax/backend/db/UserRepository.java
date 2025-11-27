package parallax.backend.db;

import parallax.backend.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserRepository {
    private final Map<String, User> users = new HashMap<>();

    public UserRepository() {
        // TODO: replace in-memory map with real SQLite queries using DataSource
        User demo = new User("demo@parallax.test", "demo@parallax.test", "Demo User", "DemoPass123");
        users.put(demo.getUsername().toLowerCase(), demo);
    }

    public Optional<User> findByIdentifierAndPassword(String identifier, String password) {
        if (identifier == null || password == null) {
            return Optional.empty();
        }
        // TODO: replace in-memory map with real SQLite queries using DataSource
        String key = identifier.toLowerCase();
        User user = users.get(key);
        if (user != null && password.equals(user.getPassword())) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
