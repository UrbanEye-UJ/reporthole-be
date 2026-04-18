package za.co.urbaneye.reporthole.global.entity;

import java.time.LocalDateTime;

public record ErrorObject(
        String message,
        int status,
        LocalDateTime timestamp
) {}
