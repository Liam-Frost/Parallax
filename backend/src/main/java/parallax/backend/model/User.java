package parallax.backend.model;

public class User {
    private String username;
    private String email;
    private String displayName;
    private String password; // TODO: hash passwords when real auth is implemented

    public User() {
    }

    public User(String username, String email, String displayName, String password) {
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
