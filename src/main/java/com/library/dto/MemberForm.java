package com.library.dto;

import com.library.model.User;

public class MemberForm {
    private String name = "";
    private String email = "";
    private String membershipId = "";
    private String password = "";

    public static MemberForm of(User user) {
        MemberForm f = new MemberForm();
        f.name = user.getName();
        f.email = user.getEmail();
        f.membershipId = user.getMembershipId() == null ? "" : user.getMembershipId();
        return f;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMembershipId() { return membershipId; }
    public void setMembershipId(String membershipId) { this.membershipId = membershipId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
