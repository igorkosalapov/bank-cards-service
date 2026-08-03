package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "crypto.aes-key-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class TransferConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransferService transferService;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private UserRepository userRepository;

    private UserPrincipal principal;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
        cardRepository.deleteAll();

        User owner = userRepository.findByUsername("user")
                .orElseThrow();

        principal = UserPrincipal.from(owner);

        fromCard = cardRepository.saveAndFlush(
                card(owner, "1111", new BigDecimal("100.00"))
        );
        toCard = cardRepository.saveAndFlush(
                card(owner, "2222", BigDecimal.ZERO)
        );
    }

    @Test
    void concurrentTransfers_onlyOneSucceeds_andTotalBalanceIsPreserved()
            throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<String> transferAttempt = () -> {
            ready.countDown();

            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent start timed out");
            }

            try {
                transferService.transferBetweenOwnCards(
                        principal,
                        new TransferRequest(
                                fromCard.getId(),
                                toCard.getId(),
                                new BigDecimal("80.00")
                        )
                );
                return "SUCCESS";
            } catch (BadRequestException ex) {
                return ex.getMessage();
            }
        };

        try {
            Future<String> first = executor.submit(transferAttempt);
            Future<String> second = executor.submit(transferAttempt);

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<String> results = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );

            assertEquals(1, results.stream()
                    .filter("SUCCESS"::equals)
                    .count());

            assertEquals(1, results.stream()
                    .filter("Insufficient funds"::equals)
                    .count());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        Card actualFrom = cardRepository.findById(fromCard.getId())
                .orElseThrow();
        Card actualTo = cardRepository.findById(toCard.getId())
                .orElseThrow();

        assertEquals(
                new BigDecimal("20.00"),
                actualFrom.getBalance()
        );
        assertEquals(
                new BigDecimal("80.00"),
                actualTo.getBalance()
        );
        assertEquals(
                new BigDecimal("100.00"),
                actualFrom.getBalance().add(actualTo.getBalance())
        );
        assertEquals(1L, transferRepository.count());
    }

    private static Card card(
            User owner,
            String last4,
            BigDecimal balance
    ) {
        Card card = new Card();
        card.setOwner(owner);
        card.setEncryptedNumber("encrypted-" + last4);
        card.setLast4(last4);
        card.setCardholderName("Demo User");
        card.setExpirationDate(LocalDate.now().plusYears(1));
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(balance);
        return card;
    }
}