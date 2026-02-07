package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.util.TransferMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class TransferService {

    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;
    private final UserRepository userRepository;

    public TransferService(CardRepository cardRepository, TransferRepository transferRepository, UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.transferRepository = transferRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TransferResponse transferBetweenOwnCards(UserPrincipal principal, TransferRequest req) {
        if (req.getFromCardId().equals(req.getToCardId())) {
            throw new BadRequestException("fromCardId and toCardId must be different");
        }

        Long firstId = req.getFromCardId() < req.getToCardId() ? req.getFromCardId() : req.getToCardId();
        Long secondId = req.getFromCardId() < req.getToCardId() ? req.getToCardId() : req.getFromCardId();

        Card first = cardRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        Card second = cardRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        Card from = req.getFromCardId().equals(firstId) ? first : second;
        Card to = req.getToCardId().equals(firstId) ? first : second;

        if (!from.getOwner().getId().equals(principal.getId()) || !to.getOwner().getId().equals(principal.getId())) {
            throw new ForbiddenException("Transfers allowed only between your own cards");
        }

        ensureActive(from);
        ensureActive(to);

        BigDecimal amount = req.getAmount();
        if (amount.scale() > 2) {
            throw new BadRequestException("amount must have max 2 decimal places");
        }

        if (from.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        User createdBy = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Transfer t = new Transfer();
        t.setFromCard(from);
        t.setToCard(to);
        t.setAmount(amount);
        t.setCreatedBy(createdBy);

        transferRepository.save(t);
        return TransferMapper.toResponse(t);
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> listMyTransfers(UserPrincipal principal, int page, int size) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return transferRepository.findByCreatedBy(user, pageable)
                .map(TransferMapper::toResponse);
    }

    private void ensureActive(Card card) {
        if (card.getExpirationDate() != null && card.getExpirationDate().isBefore(LocalDate.now())) {
            card.setStatus(CardStatus.EXPIRED);
        }

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new BadRequestException("Card is not active: " + card.getStatus());
        }
    }
}
