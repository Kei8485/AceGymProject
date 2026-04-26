package codes.acegym;

/**
 * Simple session singleton.
 * Set the logged-in username once (in LoginController) and read it anywhere.
 */
public class Session {

    private static Session instance;

    private String loggedInUsername;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) instance = new Session();
        return instance;
    }

    public String getLoggedInUsername() { return loggedInUsername; }
    public void   setLoggedInUsername(String u) { loggedInUsername = u; }

    public void clear() { loggedInUsername = null; }
}