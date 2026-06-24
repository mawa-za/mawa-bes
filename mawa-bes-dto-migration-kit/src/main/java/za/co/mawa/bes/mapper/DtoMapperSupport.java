package za.co.mawa.bes.mapper;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class DtoMapperSupport {

    private DtoMapperSupport() {
    }

    public static <S, T> List<T> mapList(List<S> source, Function<S, T> mapper) {
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .map(mapper)
                .toList();
    }
}
