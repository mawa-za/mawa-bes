package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.MembershipEntity;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MembershipUpdateHandlerRegistry {
    private final List<MembershipUpdateHandler> handlers;
    public void handleUpdate(String membership) {
        handlers.stream()
                .findFirst()
                .ifPresent(handler -> handler.onUpdate(membership));
    }
}
