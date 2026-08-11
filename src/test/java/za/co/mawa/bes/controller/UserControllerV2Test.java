package za.co.mawa.bes.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.dto.user.UserQueryDto;
import za.co.mawa.bes.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerV2Test {

    @Test
    void getUsersBindsFiltersFromQueryParametersAndReturnsJsonArray() throws Exception {
        UserService userService = mock(UserService.class);
        UserDto user = new UserDto();
        user.setId("user-1");
        user.setUsername("cashier.one");
        user.setStatus("ACTIVE");
        when(userService.getAll(any(UserQueryDto.class))).thenReturn(List.of(user));

        UserControllerV2 controller = new UserControllerV2();
        ReflectionTestUtils.setField(controller, "userService", userService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/v2/user").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("user-1"))
                .andExpect(jsonPath("$[0].username").value("cashier.one"));

        org.mockito.ArgumentCaptor<UserQueryDto> query =
                org.mockito.ArgumentCaptor.forClass(UserQueryDto.class);
        verify(userService).getAll(query.capture());
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", query.getValue().getStatus());
    }
}
