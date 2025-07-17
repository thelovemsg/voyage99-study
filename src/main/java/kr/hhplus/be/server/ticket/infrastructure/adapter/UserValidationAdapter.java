package kr.hhplus.be.server.ticket.infrastructure.adapter;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.ticket.domain.port.UserValidationPort;
import kr.hhplus.be.server.user.domain.UserEntity;
import kr.hhplus.be.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class UserValidationAdapter implements UserValidationPort {

    private final UserRepository userRepository;

    @Override
    public UserEntity validateAndGetUser(Long userId, BigDecimal amount) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ParameterNotValidException(MessageCode.USER_NOT_FOUND));

        if(!user.hasEnoughPoint(amount))
            throw new ParameterNotValidException(MessageCode.USER_POINT_NOT_ENOUGH);

        return user;
    }

    @Override
    public void saveUser(UserEntity user) {
        userRepository.save(user);  // 저장 구현
    }
}
