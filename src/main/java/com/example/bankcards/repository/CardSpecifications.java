package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import jakarta.persistence.criteria.Path;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<Card> ownerIs(User owner) {
        return (root, query, cb) -> cb.equal(root.get("owner"), owner);
    }

    public static Specification<Card> statusIs(CardStatus status) {
        return (root, query, cb) -> {
            // Статус "EXPIRED" определяется не только полем status, но и датой окончания.
            // Это позволяет фильтровать корректно, даже если статус в БД ещё не обновлялся.
            Path<LocalDate> exp = root.get("expirationDate");
            Path<CardStatus> st = root.get("status");
            LocalDate today = LocalDate.now();

            if (status == CardStatus.EXPIRED) {
                return cb.or(
                        cb.lessThan(exp, today),
                        cb.equal(st, CardStatus.EXPIRED)
                );
            }

            // ACTIVE/BLOCKED: исключаем истёкшие по дате карты
            return cb.and(
                    cb.equal(st, status),
                    cb.greaterThanOrEqualTo(exp, today)
            );
        };
    }

    public static Specification<Card> search(String text) {
        return (root, query, cb) -> {
            String like = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("last4")), like),
                    cb.like(cb.lower(root.get("cardholderName")), like)
            );
        };
    }
}
