package com.loganalyzer.model;

/**
 * Role enumeration for user authorization
 */
public enum Role {
    ADMIN("ROLE_ADMIN", "Administrator with full system access"),
    DEVELOPER("ROLE_DEVELOPER", "Developer with code and deployment access"),
    QA("ROLE_QA", "Quality Assurance with testing and monitoring access");
    
    private final String authority;
    private final String description;
    
    Role(String authority, String description) {
        this.authority = authority;
        this.description = description;
    }
    
    public String getAuthority() {
        return authority;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return authority;
    }
}
