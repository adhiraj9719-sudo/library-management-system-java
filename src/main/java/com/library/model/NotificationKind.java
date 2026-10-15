package com.library.model;

public enum NotificationKind {
    GENERAL("General announcement"),
    DUE_DATE("Due date"),
    NEW_ARRIVAL("New arrival");

    private final String label;

    NotificationKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
