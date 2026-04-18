package za.co.urbaneye.reporthole.user.service.interfaces;

import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;

public interface IUserAuthService {
    public void registerUser(final RegisterRequest user);
    public String loginUser(final LoginRequest user);
}
