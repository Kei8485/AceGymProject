package codes.acegym.DB;

/**
 * Gmail SMTP credentials for the renewal notification service.
 *
 * HOW TO FILL THIS IN:
 *  1. Go to myaccount.google.com → Security → App Passwords
 *  2. Generate a new app password for "Mail / Windows Computer"
 *  3. Paste the 16-character code into APP_PASSWORD below (spaces are fine)
 *  4. Replace SENDER_EMAIL with the Gmail address you used above
 */
public class GmailConfig {

    /** The Gmail address that sends the notification emails. */
    public static final String SENDER_EMAIL = "ynandandrei.bautista.200625@gmail.com";   // ← replace

    /** 16-character App Password from Google (NOT your normal Gmail password). */
    public static final String APP_PASSWORD  = "bpxg oyhk gnfn zqbv";  // ← replace

    /** Display name shown in the recipient's inbox as the sender. */
    public static final String SENDER_NAME   = "AceGym";
}