package com.ezlevup.dentalchat.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    CUSTOMER,
    ADMIN;
    
    @JsonCreator
    public static UserRole fromString(String value) {
        if (value == null) {
            return null;
        }
        return UserRole.valueOf(value.toUpperCase());
    }
    
    @JsonValue
    public String getValue() {
        return this.name();
    }
}