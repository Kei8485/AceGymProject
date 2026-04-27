package codes.acegym.DB;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * RenewalNotificationService
 *
 * Two modes:
 *  • run()        — bulk: queries all expiring members at startup, emails each one
 *  • sendToOne()  — single: called from the Dashboard button for one client
 */
public class RenewalNotificationService {

    // ── Internal data carrier ─────────────────────────────────────────────────
    public record RenewalTarget(
            String fullName,
            String email,
            String expiryDate,
            int    daysLeft
    ) {}

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC — BULK (startup)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Run at app startup alongside ExpiryResetDAO.runExpiryResets().
     * @return number of emails successfully sent
     */
    public static int run() {
        List<RenewalTarget> targets = getExpiringClients();
        if (targets.isEmpty()) {
            System.out.println("[RenewalNotification] No memberships expiring within 7 days.");
            return 0;
        }
        int sent = 0;
        for (RenewalTarget t : targets) {
            if (isEmailMissing(t.email())) {
                System.out.println("[RenewalNotification] Skipped (no email): " + t.fullName());
                continue;
            }
            if (sendRenewalEmail(t)) sent++;
        }
        System.out.println("[RenewalNotification] Done — " + sent + "/" + targets.size() + " sent.");
        return sent;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC — SINGLE (Dashboard button)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Send a renewal reminder to one specific client.
     * Called directly from the Dashboard renewal row button.
     *
     * @param fullName   client full name
     * @param email      client email address
     * @param expiryDate formatted string e.g. "Apr 28, 2026"
     * @param daysLeft   days until expiry (0 = today)
     * @return true if sent successfully
     */
    public static boolean sendToOne(String fullName, String email,
                                    String expiryDate, int daysLeft) {
        if (isEmailMissing(email)) {
            System.out.println("[RenewalNotification] No email on file for: " + fullName);
            return false;
        }
        return sendRenewalEmail(new RenewalTarget(fullName, email, expiryDate, daysLeft));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DATABASE QUERY
    // ═════════════════════════════════════════════════════════════════════════

    private static List<RenewalTarget> getExpiringClients() {
        List<RenewalTarget> list = new ArrayList<>();
        String sql =
                "SELECT CONCAT(c.FirstName, ' ', c.LastName) AS FullName, " +
                        "       c.ClientEmail, " +
                        "       DATE_FORMAT(m.DateExpired, '%b %d, %Y') AS ExpiryDate, " +
                        "       DATEDIFF(m.DateExpired, CURDATE())       AS DaysLeft " +
                        "FROM MembershipTable m " +
                        "JOIN ClientTable c ON c.ClientID = m.ClientID " +
                        "WHERE m.DateExpired >= CURDATE() " +
                        "  AND m.DateExpired <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) " +
                        "  AND m.ClientTypeID = 2 " +
                        "  AND c.ClientEmail IS NOT NULL AND c.ClientEmail != '' " +
                        "  AND m.MembershipID = (" +
                        "      SELECT MembershipID FROM MembershipTable " +
                        "      WHERE ClientID = m.ClientID " +
                        "      ORDER BY DateApplied DESC LIMIT 1" +
                        "  ) " +
                        "ORDER BY m.DateExpired ASC";

        try (Connection con = DBConnector.connect();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new RenewalTarget(
                        rs.getString("FullName"),
                        rs.getString("ClientEmail"),
                        rs.getString("ExpiryDate"),
                        rs.getInt("DaysLeft")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[RenewalNotification] DB query failed: " + e.getMessage());
        }
        return list;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // EMAIL SENDING
    // ═════════════════════════════════════════════════════════════════════════

    private static boolean sendRenewalEmail(RenewalTarget t) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",              "true");
        props.put("mail.smtp.starttls.enable",   "true");
        props.put("mail.smtp.host",              "smtp.gmail.com");
        props.put("mail.smtp.port",              "587");
        props.put("mail.smtp.ssl.trust",         "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout",           "8000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        GmailConfig.SENDER_EMAIL,
                        GmailConfig.APP_PASSWORD
                );
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(GmailConfig.SENDER_EMAIL, GmailConfig.SENDER_NAME));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(t.email()));
            msg.setSubject("⏰ Your AceGym Membership Expires Soon — Renew Now");
            msg.setContent(buildHtml(t), "text/html; charset=utf-8");
            Transport.send(msg);
            System.out.println("[RenewalNotification] ✅ Sent to: " + t.email());
            return true;
        } catch (Exception e) {
            System.err.println("[RenewalNotification] ❌ Failed for " + t.email() + ": " + e.getMessage());
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HTML EMAIL TEMPLATE
    // ═════════════════════════════════════════════════════════════════════════

    private static String buildHtml(RenewalTarget t) {
        String daysLabel = switch (t.daysLeft()) {
            case 0  -> "TODAY";
            case 1  -> "TOMORROW";
            default -> "IN " + t.daysLeft() + " DAYS";
        };
        String urgencyColor = t.daysLeft() <= 1 ? "#dc3545" : "#e94560";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body { background-color: #f0f0f0; font-family: Arial, Helvetica, sans-serif; }
                    .wrapper { max-width: 580px; margin: 40px auto; background: #fff;
                               border-radius: 12px; overflow: hidden;
                               box-shadow: 0 6px 24px rgba(0,0,0,0.12); }
                    .header { background: #1a1a2e; padding: 36px 32px; text-align: center; }
                    .header .logo { font-size: 30px; font-weight: 900; color: #e94560; letter-spacing: 2px; }
                    .header .tagline { color: #aaa; font-size: 13px; margin-top: 6px; }
                    .banner { background: %s; padding: 14px; text-align: center;
                              color: #fff; font-size: 15px; font-weight: bold; letter-spacing: 1px; }
                    .body { padding: 36px 32px; }
                    .body p { color: #444; font-size: 15px; line-height: 1.7; margin-bottom: 16px; }
                    .name { font-weight: bold; color: #1a1a2e; }
                    .card { background: #f9f9f9; border-left: 4px solid %s;
                            border-radius: 6px; padding: 18px 20px; margin: 20px 0; }
                    .card-row { display: flex; justify-content: space-between;
                                align-items: center; margin-bottom: 10px; }
                    .card-row:last-child { margin-bottom: 0; }
                    .card-label { color: #888; font-size: 13px; }
                    .card-value { color: #1a1a2e; font-weight: bold; font-size: 14px; }
                    .expiry-value { color: %s; font-size: 16px; }
                    .cta-wrap { text-align: center; margin: 28px 0 8px; }
                    .cta { display: inline-block; background: #e94560; color: #fff;
                           text-decoration: none; padding: 14px 36px; border-radius: 8px;
                           font-size: 15px; font-weight: bold; }
                    .footer { background: #f0f0f0; padding: 20px 32px; text-align: center;
                              font-size: 12px; color: #999; line-height: 1.6; }
                  </style>
                </head>
                <body>
                  <div class="wrapper">
                    <div class="header">
                      <div class="logo">⚡ ACEGYM</div>
                      <div class="tagline">Membership Renewal Reminder</div>
                    </div>
                    <div class="banner">🔔 YOUR MEMBERSHIP EXPIRES %s</div>
                    <div class="body">
                      <p>Hi <span class="name">%s</span>,</p>
                      <p>Your AceGym membership is expiring very soon. Once it expires, your
                         training plan access and coach assignment will be reset.</p>
                      <div class="card">
                        <div class="card-row">
                          <span class="card-label">Member Name</span>
                          <span class="card-value">%s</span>
                        </div>
                        <div class="card-row">
                          <span class="card-label">Expiry Date</span>
                          <span class="card-value expiry-value">%s</span>
                        </div>
                        <div class="card-row">
                          <span class="card-label">Status</span>
                          <span class="card-value" style="color:#28a745;">● Active</span>
                        </div>
                      </div>
                      <p>Renew before your expiry date to keep uninterrupted access to all
                         your gym benefits, training programs, and coach support.</p>
                      <div class="cta-wrap">
                        <a class="cta" href="#">Renew My Membership</a>
                      </div>
                    </div>
                    <div class="footer">
                      <p>This is an automated reminder from <strong>AceGym</strong>.</p>
                      <p>© 2025 AceGym. All rights reserved.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                urgencyColor, urgencyColor, urgencyColor,
                daysLabel, t.fullName(), t.fullName(), t.expiryDate()
        );
    }

    private static boolean isEmailMissing(String email) {
        return email == null || email.isBlank() || email.equals("—");
    }
}