package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransferServiceTest {

    private CardRepository cardRepository;
    private TransferRepository transferRepository;
    private UserRepository userRepository;

    private TransferService transferService;

    private User owner;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        transferRepository = mock(TransferRepository.class);
        userRepository = mock(UserRepository.class);

        transferService = new TransferService(cardRepository, transferRepository, userRepository);

        owner = new User();
        owner.setId(1L);
        owner.setUsername("user");
        owner.setRole(Role.USER);
        owner.setEnabled(true);

        principal = new UserPrincipal(1L, "user", "hash", "USER", true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        doAnswer(inv -> {
            var t = inv.getArgument(0, com.example.bankcards.entity.Transfer.class);
            t.setId(42L);
            return t;
        }).when(transferRepository).save(any());
    }

    @Test
    void transfer_success_updatesBalances_andPersistsTransfer() {
        Card from = card(10L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(100.00), LocalDate.now().plusMonths(2));
        Card to = card(20L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(10.00), LocalDate.now().plusMonths(3));

        when(cardRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(to));

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(new BigDecimal("25.50"));

        var resp = transferService.transferBetweenOwnCards(principal, req);

        assertEquals(42L, resp.getId());
        assertEquals(10L, resp.getFromCardId());
        assertEquals(20L, resp.getToCardId());
        assertEquals(new BigDecimal("25.50"), resp.getAmount());

        assertEquals(new BigDecimal("74.50"), from.getBalance());
        assertEquals(new BigDecimal("35.50"), to.getBalance());

        ArgumentCaptor<com.example.bankcards.entity.Transfer> captor = ArgumentCaptor.forClass(com.example.bankcards.entity.Transfer.class);
        verify(transferRepository).save(captor.capture());
        assertEquals(from, captor.getValue().getFromCard());
        assertEquals(to, captor.getValue().getToCard());
        assertEquals(owner, captor.getValue().getCreatedBy());
    }

    @Test
    void transfer_fails_onInsufficientFunds() {
        Card from = card(10L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(5), LocalDate.now().plusMonths(2));
        Card to = card(20L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(10), LocalDate.now().plusMonths(3));

        when(cardRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(to));

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(new BigDecimal("10.00"));

        assertThrows(BadRequestException.class, () -> transferService.transferBetweenOwnCards(principal, req));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_fails_whenNotOwner() {
        User other = new User();
        other.setId(2L);
        other.setUsername("other");
        other.setRole(Role.USER);
        other.setEnabled(true);

        Card from = card(10L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(100), LocalDate.now().plusMonths(2));
        Card to = card(20L, other, CardStatus.ACTIVE, BigDecimal.valueOf(10), LocalDate.now().plusMonths(3));

        when(cardRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(to));

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(new BigDecimal("10.00"));

        assertThrows(ForbiddenException.class, () -> transferService.transferBetweenOwnCards(principal, req));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_fails_whenFromBlocked() {
        Card from = card(10L, owner, CardStatus.BLOCKED, BigDecimal.valueOf(100), LocalDate.now().plusMonths(2));
        Card to = card(20L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(10), LocalDate.now().plusMonths(3));

        when(cardRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(to));

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(new BigDecimal("10.00"));

        assertThrows(BadRequestException.class, () -> transferService.transferBetweenOwnCards(principal, req));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_fails_whenAmountHasMoreThanTwoDecimals() {
        Card from = card(10L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(100), LocalDate.now().plusMonths(2));
        Card to = card(20L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(10), LocalDate.now().plusMonths(3));

        when(cardRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(to));

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(new BigDecimal("1.234"));

        assertThrows(BadRequestException.class, () -> transferService.transferBetweenOwnCards(principal, req));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_fails_whenSameCardIds() {
        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(10L);
        req.setAmount(new BigDecimal("1.00"));

        assertThrows(BadRequestException.class, () -> transferService.transferBetweenOwnCards(principal, req));
        verifyNoInteractions(cardRepository);
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_fails_whenToBlocked() {
        Card from = card(10L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(100), LocalDate.now().plusMonths(2));
        Card to = card(20L, owner, CardStatus.BLOCKED, BigDecimal.valueOf(10), LocalDate.now().plusMonths(3));

        when(cardRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(to));

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(new BigDecimal("1.00"));

        assertThrows(BadRequestException.class, () -> transferService.transferBetweenOwnCards(principal, req));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_fails_whenToExpiredByDate() {
        Card from = card(10L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(100), LocalDate.now().plusMonths(2));
        Card to = card(20L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(10), LocalDate.now().minusDays(1));

        when(cardRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(to));

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(new BigDecimal("1.00"));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> transferService.transferBetweenOwnCards(principal, req));
        assertTrue(ex.getMessage().contains("EXPIRED"));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_fails_whenCardNotFound() {
        when(cardRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(new BigDecimal("1.00"));

        assertThrows(NotFoundException.class, () -> transferService.transferBetweenOwnCards(principal, req));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_fails_whenUserNotFound() {
        Card from = card(10L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(100.00), LocalDate.now().plusMonths(2));
        Card to = card(20L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(10.00), LocalDate.now().plusMonths(3));

        when(cardRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(from));
        when(cardRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(to));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        TransferRequest req = new TransferRequest();
        req.setFromCardId(10L);
        req.setToCardId(20L);
        req.setAmount(new BigDecimal("1.00"));

        assertThrows(NotFoundException.class, () -> transferService.transferBetweenOwnCards(principal, req));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void listMyTransfers_returnsPage() {
        Card from = card(10L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(100.00), LocalDate.now().plusMonths(2));
        Card to = card(20L, owner, CardStatus.ACTIVE, BigDecimal.valueOf(10.00), LocalDate.now().plusMonths(3));

        Transfer t = new Transfer();
        t.setId(7L);
        t.setFromCard(from);
        t.setToCard(to);
        t.setAmount(new BigDecimal("12.34"));
        t.setCreatedBy(owner);
        t.setCreatedAt(Instant.parse("2026-01-01T10:15:30Z"));

        when(transferRepository.findByCreatedBy(eq(owner), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        Page<?> page = transferService.listMyTransfers(principal, 0, 20);
        assertEquals(1, page.getTotalElements());
        var dto = (com.example.bankcards.dto.TransferResponse) page.getContent().get(0);
        assertEquals(7L, dto.getId());
        assertEquals(10L, dto.getFromCardId());
        assertEquals(20L, dto.getToCardId());
        assertEquals(new BigDecimal("12.34"), dto.getAmount());
        assertEquals(Instant.parse("2026-01-01T10:15:30Z"), dto.getCreatedAt());
    }

    private static Card card(Long id, User owner, CardStatus status, BigDecimal balance, LocalDate exp) {
        Card c = new Card();
        c.setId(id);
        c.setOwner(owner);
        c.setStatus(status);
        c.setBalance(balance);
        c.setExpirationDate(exp);
        c.setLast4("1234");
        c.setEncryptedNumber("enc");
        c.setCardholderName("Demo");
        return c;
    }
}
