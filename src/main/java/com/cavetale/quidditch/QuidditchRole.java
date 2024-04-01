package com.cavetale.quidditch;

import lombok.Getter;

@Getter
public enum QuidditchRole {
    SEEKER,
    CHASER, // Score goals
    BEATER, //
    KEEPER;

    private final String humanName;

    QuidditchRole() {
        this.humanName = name().substring(0, 1) + name().substring(1).toLowerCase();
    }
}
