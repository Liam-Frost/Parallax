package parallax.backend.db;

import parallax.backend.model.User;

import java.util.Map;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByIdentifierAndPassword(String identifier, String password);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phoneCountry, String phoneDigits);

    User createUser(User user);

    Optional<User> updateContact(String username, String newEmail, String phoneCountry, String phone);

    Optional<User> updatePassword(String username, String newPassword);

    boolean deleteUser(String username);

    Map<String, User> findAllUsers();
}
