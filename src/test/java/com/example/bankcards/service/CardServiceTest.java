package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CardServiceTest {

    private CardRepository cardRepository;
    private UserRepository userRepository;
    private CryptoUtil cryptoUtil;

    private CardService cardService;

    private User owner;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        userRepository = mock(UserRepository.class);
        cryptoUtil = mock(CryptoUtil.class);

        cardService = new CardService(cardRepository, userRepository, cryptoUtil);

        owner = new User();
        owner.setId(1L);
        owner.setUsername("user");
        owner.setFullName("Иван Иванов");
        owner.setRole(Role.USER);
        owner.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        doAnswer(inv -> {
            Card c = inv.getArgument(0, Card.class);
            c.setId(100L);
            return c;
        }).when(cardRepository).save(any(Card.class));
    }

    @Test
    void create_encryptsNumber_setsLast4_defaultsHolderAndBalance() {
        when(cryptoUtil.encrypt("4111111111111111")).thenReturn("enc");

        CardCreateRequest req = new CardCreateRequest();
        req.setOwnerId(1L);
        req.setCardNumber("4111111111111111");
        req.setCardholderName(null);
        req.setExpiration("2027-02");
        req.setInitialBalance(new BigDecimal("12.34"));

        var resp = cardService.create(req);

        assertEquals(100L, resp.getId());
        assertEquals("**** **** **** 1111", resp.getMaskedNumber());
        assertEquals("Иван Иванов", resp.getCardholderName());
        assertEquals("02/27", resp.getExpiration());
        assertEquals("ACTIVE", resp.getStatus());
        assertEquals(new BigDecimal("12.34"), resp.getBalance());
        assertFalse(resp.isBlockRequested());
        assertEquals("user", resp.getOwnerUsername());

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(captor.capture());
        Card saved = captor.getValue();

        assertEquals(owner, saved.getOwner());
        assertEquals("enc", saved.getEncryptedNumber());
        assertEquals("1111", saved.getLast4());
        assertEquals(CardStatus.ACTIVE, saved.getStatus());
        assertEquals(new BigDecimal("12.34"), saved.getBalance());
        assertEquals(YearMonth.of(2027, 2).atEndOfMonth(), saved.getExpirationDate());
    }

    @Test
    void create_fails_whenOwnerNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        CardCreateRequest req = new CardCreateRequest();
        req.setOwnerId(1L);
        req.setCardNumber("4111111111111111");
        req.setExpiration("2027-02");

        assertThrows(NotFoundException.class, () -> cardService.create(req));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void create_fails_whenInitialBalanceNegative() {
        when(cryptoUtil.encrypt("4111111111111111")).thenReturn("enc");

        CardCreateRequest req = new CardCreateRequest();
        req.setOwnerId(1L);
        req.setCardNumber("4111111111111111");
        req.setExpiration("2027-02");
        req.setInitialBalance(new BigDecimal("-0.01"));

        assertThrows(BadRequestException.class, () -> cardService.create(req));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void requestBlock_setsFlag_forActiveCard() {
        Card card = card(10L, owner, CardStatus.ACTIVE, LocalDate.now().plusMonths(1));
        when(cardRepository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(card));

        var resp = cardService.requestBlock(10L, owner);

        assertTrue(resp.isBlockRequested());
        assertTrue(card.isBlockRequested());
    }

    @Test
    void requestBlock_fails_forExpiredCard() {
        Card card = card(10L, owner, CardStatus.ACTIVE, LocalDate.now().minusDays(1));
        when(cardRepository.findByIdAndOwner(10L, owner)).thenReturn(Optional.of(card));

        assertThrows(BadRequestException.class, () -> cardService.requestBlock(10L, owner));
    }

    @Test
    void updateStatus_cannotSetExpiredManually() {
        assertThrows(BadRequestException.class, () -> cardService.updateStatus(10L, "EXPIRED"));
    }

    @Test
    void updateStatus_forcesExpired_whenExpiredByDate() {
        Card card = card(10L, owner, CardStatus.ACTIVE, LocalDate.now().minusDays(1));
        card.setBlockRequested(true);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        var resp = cardService.updateStatus(10L, "ACTIVE");

        assertEquals("EXPIRED", resp.getStatus());
        assertFalse(resp.isBlockRequested());
        assertEquals(CardStatus.EXPIRED, card.getStatus());
        assertFalse(card.isBlockRequested());
    }

    @Test
    void updateStatus_clearsBlockRequested_onActivate() {
        Card card = card(10L, owner, CardStatus.BLOCKED, LocalDate.now().plusMonths(2));
        card.setBlockRequested(true);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        var resp = cardService.updateStatus(10L, "ACTIVE");

        assertEquals("ACTIVE", resp.getStatus());
        assertFalse(resp.isBlockRequested());
        assertEquals(CardStatus.ACTIVE, card.getStatus());
        assertFalse(card.isBlockRequested());
    }

    private static Card card(Long id, User owner, CardStatus status, LocalDate exp) {
        Card c = new Card();
        c.setId(id);
        c.setOwner(owner);
        c.setStatus(status);
        c.setBalance(BigDecimal.ZERO);
        c.setExpirationDate(exp);
        c.setLast4("1234");
        c.setEncryptedNumber("enc");
        c.setCardholderName("Demo");
        return c;
    }
}
