package kr.hhplus.be.server.user.dto;

import kr.hhplus.be.server.user.domain.UserEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserCreateDto {

    @Getter
    @Setter
    public static class Request {
        private String userName;
        private BigDecimal pointAmount;

        public static UserEntity toEntity(Request request) {
            return UserEntity.builder()
                    .userName(request.getUserName())
                    .pointAmount(request.getPointAmount())
                    .build();
        }
    }
}
