package za.co.urbaneye.reporthole.user.dto;

public record RegisterRequest(String firstName, String lastName, String email,String emailHash,String role, String password, String phoneNumber){};
