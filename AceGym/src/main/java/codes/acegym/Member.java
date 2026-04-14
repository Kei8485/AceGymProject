package codes.acegym;

import java.time.LocalDate;

public class Member {
    private int memberID;
    private String name;
    private LocalDate dateEnrolled;
    private LocalDate dateExpiration;

    public Member(int memberID, String name, LocalDate dateEnrolled, LocalDate dateExpiration) {
        this.memberID = memberID;
        this.name = name;
        this.dateEnrolled = dateEnrolled;
        this.dateExpiration = dateExpiration;
    }

    public int getMemberID() { return memberID; }
    public String getName() { return name; }
    public LocalDate getDateEnrolled() { return dateEnrolled; }
    public LocalDate getDateExpiration() { return dateExpiration; }
}