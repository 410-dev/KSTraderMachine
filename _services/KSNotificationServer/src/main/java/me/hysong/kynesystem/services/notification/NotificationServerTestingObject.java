package me.hysong.kynesystem.services.notification;


import lombok.Getter;

import java.io.Serializable;

@Getter
public class NotificationServerTestingObject implements Serializable {
    private final String message;

    public NotificationServerTestingObject(String message) {
        this.message = message;
    }

}
