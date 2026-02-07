package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<Card> ownerIs(User owner) {
        return (root, query, cb) -> cb.equal(root.get("owner"), owner);
    }

    public static Specification<Card> statusIs(CardStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Card> blockRequestedIs(Boolean blockRequested) {
        return (root, query, cb) -> cb.equal(root.get("blockRequested"), blockRequested);
    }

    /**
     * Simple search by last4 or cardholderName.
     */
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
