package kr.hhplus.be.server.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginationUtils {

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 20);

    public static <T> PageImpl<T> toPage(Pageable pageable, Function<Pageable, List<T>> contentSupplier, LongSupplier totalSupplier) {

        Pageable safePageable = ObjectUtils.isEmpty(pageable) ? DEFAULT_PAGEABLE : pageable;

        List<T> content = contentSupplier.apply(safePageable);

        int size = pageable.getPageSize();
        int page = pageable.getPageNumber();

        long totalCount;
        if (page == 0 && content.size() < size) {
            totalCount = content.size();
        } else {
            totalCount = totalSupplier.getAsLong();
        }

        return new PageImpl<>(content, pageable, totalCount);
    }
}