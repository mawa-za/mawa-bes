package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.dto.user.UserQueryDto;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceListTest {

    @Mock private UserRepository userRepository;
    @Mock private PartnerService partnerService;

    @Test
    @SuppressWarnings("unchecked")
    void getAllDoesNotExpandPartnerForEveryUser() throws Exception {
        UserService service = new UserService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "partnerService", partnerService);

        UserEntity entity = UserEntity.builder()
                .id("user-1")
                .username("cashier.one")
                .partner("partner-1")
                .email("cashier@example.com")
                .cellphone("0712345678")
                .password("encrypted".getBytes(StandardCharsets.UTF_8))
                .userType("ADMIN")
                .status("ACTIVE")
                .accountType("STANDARD")
                .testUser(false)
                .protectedUser(false)
                .systemManaged(false)
                .accessScope("STANDARD")
                .externalTransactionsBlocked(false)
                .mfaRequired(false)
                .build();

        when(userRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(entity));

        List<UserDto> users = service.getAll(new UserQueryDto());

        assertEquals(1, users.size());
        assertEquals("user-1", users.get(0).getId());
        assertEquals("cashier.one", users.get(0).getUsername());
        assertNull(users.get(0).getPassword());
        assertNull(users.get(0).getPartner());
        verify(partnerService, never()).get(any(String.class));
    }
}
