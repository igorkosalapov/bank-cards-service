package com.example.bankcards.service;

import com.example.bankcards.dto.BalanceResponse;
import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardSpecifications;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.CardNumberUtil;
import com.example.bankcards.util.CryptoUtil;
import com.example.bankcards.util.ExpirationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CryptoUtil cryptoUtil;

    public CardService(CardRepository cardRepository, UserRepository userRepository, CryptoUtil cryptoUtil) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.cryptoUtil = cryptoUtil;
    }

    @Transactional
    public CardResponse create(CardCreateRequest req) {
        User owner = userRepository.findById(req.getOwnerId())
                .orElseThrow(() -> new NotFoundException("Owner not found"));

        String cardNumber = req.getCardNumber();
        String last4 = CardNumberUtil.last4(cardNumber);

        Card card = new Card();
        card.setOwner(owner);
        card.setEncryptedNumber(cryptoUtil.encrypt(cardNumber));
        card.setLast4(last4);

        String holder = req.getCardholderName();
        if (holder == null || holder.isBlank()) {
            holder = owner.getFullName();
        }
        card.setCardholderName(holder);

        card.setExpirationDate(ExpirationUtil.parseToLastDayOfMonth(req.getExpiration()));
        card.setStatus(CardStatus.ACTIVE);

        BigDecimal bal = req.getInitialBalance();
        if (bal != null) {
            if (bal.signum() < 0) {
                throw new BadRequestException("initialBalance must be >= 0");
            }
            card.setBalance(bal);
        } else {
            card.setBalance(BigDecimal.ZERO);
        }

        cardRepository.save(card);
        return CardMapper.toResponse(card, true);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> listAll(String q, String statusRaw, Boolean blockRequested, Pageable pageable) {
        Specification<Card> spec = Specification.where(null);

        if (q != null && !q.isBlank()) {
            spec = spec.and(CardSpecifications.search(q.trim()));
        }
        if (statusRaw != null && !statusRaw.isBlank()) {
            spec = spec.and(CardSpecifications.statusIs(parseStatus(statusRaw)));
        }

        if (blockRequested != null) {
            spec = spec.and(CardSpecifications.blockRequestedIs(blockRequested));
        }

        Page<Card> page = cardRepository.findAll(spec, pageable);

        return page.map(c -> {
            CardStatus effective = effectiveStatus(c);
            if (effective != c.getStatus()) {

                c.setStatus(effective);
            }
            return CardMapper.toResponse(c, true);
        });
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> listMine(User owner, String q, String statusRaw, Boolean blockRequested, Pageable pageable) {
        Specification<Card> spec = Specification.where(CardSpecifications.ownerIs(owner));

        if (q != null && !q.isBlank()) {
            spec = spec.and(CardSpecifications.search(q.trim()));
        }
        if (statusRaw != null && !statusRaw.isBlank()) {
            spec = spec.and(CardSpecifications.statusIs(parseStatus(statusRaw)));
        }
        if (blockRequested != null) {
            spec = spec.and(CardSpecifications.blockRequestedIs(blockRequested));
        }

        Page<Card> page = cardRepository.findAll(spec, pageable);
        return page.map(c -> {
            CardStatus effective = effectiveStatus(c);
            if (effective != c.getStatus()) {
                c.setStatus(effective);
            }
            return CardMapper.toResponse(c, false);
        });
    }

    @Transactional(readOnly = true)
    public Card getMineEntity(Long cardId, User owner) {
        Card card = cardRepository.findByIdAndOwner(cardId, owner)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        card.setStatus(effectiveStatus(card));
        return card;
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long cardId, User owner) {
        Card card = getMineEntity(cardId, owner);
        return new BalanceResponse(card.getBalance());
    }

    @Transactional
    public CardResponse requestBlock(Long cardId, User owner) {
        Card card = cardRepository.findByIdAndOwner(cardId, owner)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        if (effectiveStatus(card) == CardStatus.EXPIRED) {
            throw new BadRequestException("Card is expired");
        }
        card.setBlockRequested(true);
        return CardMapper.toResponse(card, false);
    }

    @Transactional(readOnly = true)
    public CardResponse getMineResponse(Long cardId, User owner) {
        Card card = getMineEntity(cardId, owner);
        return CardMapper.toResponse(card, false);
    }

    @Transactional
    public CardResponse updateStatus(Long cardId, String statusRaw) {
        CardStatus newStatus = parseStatus(statusRaw);
        if (newStatus == CardStatus.EXPIRED) {
            throw new BadRequestException("Cannot set status to EXPIRED manually");
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        if (isExpiredByDate(card)) {
            card.setStatus(CardStatus.EXPIRED);
            card.setBlockRequested(false);
            return CardMapper.toResponse(card, true);
        }

        card.setStatus(newStatus);
        if (newStatus == CardStatus.ACTIVE) {

            card.setBlockRequested(false);
        }

        return CardMapper.toResponse(card, true);
    }

    @Transactional
    public void delete(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new NotFoundException("Card not found");
        }
        cardRepository.deleteById(cardId);
    }

    @Transactional(readOnly = true)
    public CardResponse getByIdAdmin(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        CardStatus effective = effectiveStatus(card);
        if (effective != card.getStatus()) {
            card.setStatus(effective);
        }
        return CardMapper.toResponse(card, true);
    }

    private CardStatus parseStatus(String raw) {
        try {
            return CardStatus.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Unsupported status: " + raw);
        }
    }

    private boolean isExpiredByDate(Card card) {
        return card.getExpirationDate() != null && card.getExpirationDate().isBefore(LocalDate.now());
    }

    private CardStatus effectiveStatus(Card card) {
        if (isExpiredByDate(card)) {
            return CardStatus.EXPIRED;
        }
        return card.getStatus();
    }

    public void assertOwner(Card card, User owner) {
        if (!card.getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException("Card does not belong to the user");
        }
    }
}
