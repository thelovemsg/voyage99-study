package kr.hhplus.be.server.user.service;

import kr.hhplus.be.server.user.domain.UserEntity;
import kr.hhplus.be.server.user.dto.UserCreateDto;
import kr.hhplus.be.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Long createUser(UserCreateDto.Request request) {
        UserEntity savedUserEntity = userRepository.save(UserCreateDto.Request.toEntity(request));
        return savedUserEntity.getUserId();
    }

}
