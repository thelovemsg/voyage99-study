package kr.hhplus.be.server.ticket.infrastructure.adapter;

import kr.hhplus.be.server.common.utils.DateUtils;
import kr.hhplus.be.server.ticket.domain.port.GenerateTicketNoPort;
import org.springframework.stereotype.Service;

/**
 * TODO : 티켓 번호는 정책을 통해서 어떤 룰을 가지고 생성할 것인지 정해서,
 * 프로시저 혹은 sequencd를 사용하면 된다. 추후 작업 한다는 가정 하에 작성만 해놓음.
 */
@Service
public class GenerateGenerateTicketNoAdapter implements GenerateTicketNoPort {

    @Override
    public String generateTicketNumber() {
        // TODO : 시퀀스 혹은 프로시저 연결 -> 티켓번호 생성 -> 반환
        return DateUtils.getDefaultDateTimeNoSplit();
    }
}
