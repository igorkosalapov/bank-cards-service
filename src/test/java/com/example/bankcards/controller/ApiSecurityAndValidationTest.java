package com.example.bankcards.controller;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.security.SecurityConfig;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        AdminUserController.class,
        AdminCardController.class,
        UserCardController.class,
        TransferController.class
})
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ApiSecurityAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @MockBean
    private UserService userService;

    @MockBean
    private TransferService transferService;

    // required by SecurityConfig / JwtAuthenticationFilter
    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void cardsEndpoint_requiresAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void adminEndpoint_forUserRole_returns403() throws Exception {
        UserPrincipal p = new UserPrincipal(1L, "user", "hash", Role.USER.name(), true);

        mockMvc.perform(get("/api/admin/users").with(user(p)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden"));

        verifyNoInteractions(userService);
    }

    @Test
    void invalidJwt_returns401_withClearMessage() throws Exception {
        when(jwtService.extractUsername("bad-token")).thenThrow(new JwtException("bad"));

        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    void transferValidationError_returns400_andDoesNotCallService() throws Exception {
        UserPrincipal p = new UserPrincipal(1L, "user", "hash", Role.USER.name(), true);

        String json = objectMapper.writeValueAsString(Map.of(
                "fromCardId", 10,
                "toCardId", 20,
                "amount", 0
        ));

        mockMvc.perform(post("/api/transfers")
                        .with(user(p))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verifyNoInteractions(transferService);
    }

    @Test
    void userCannotFetchForeignCard_returns404() throws Exception {
        UserPrincipal p = new UserPrincipal(1L, "user", "hash", Role.USER.name(), true);
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("user");

        when(userService.getEntity(1L)).thenReturn(owner);
        when(cardService.getMineResponse(eq(999L), eq(owner))).thenThrow(new NotFoundException("Card not found"));

        mockMvc.perform(get("/api/cards/999").with(user(p)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Card not found"));

        verify(cardService).getMineResponse(999L, owner);
    }

    @Test
    void adminCreateCard_validationError_returns400() throws Exception {
        UserPrincipal admin = new UserPrincipal(2L, "admin", "hash", Role.ADMIN.name(), true);

        String json = objectMapper.writeValueAsString(Map.of(
                "ownerId", 1,
                "cardNumber", "123", // invalid: must be 16 digits
                "expiration", "2027-02",
                "initialBalance", 10
        ));

        mockMvc.perform(post("/api/admin/cards")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(cardService, never()).create(any());
    }
}
