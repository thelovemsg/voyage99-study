package kr.hhplus.be.server.ticket.domain.port;

import kr.hhplus.be.server.user.domain.UserEntity;

import java.math.BigDecimal;

public interface UserValidationPort {
    UserEntity validateAndGetUser(Long userId, BigDecimal amount);
    void saveUser(UserEntity user);
}
