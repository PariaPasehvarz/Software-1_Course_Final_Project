package com.bourse.wealthwise.repository;

import com.bourse.wealthwise.domain.entity.action.BaseAction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActionRepository {

    private final Map<String, BaseAction> actions = new ConcurrentHashMap<>();

    public BaseAction save(BaseAction action) {
        String id = (action.getUuid() == null || action.getUuid().isBlank())
                ? java.util.UUID.randomUUID().toString()
                : action.getUuid();
        actions.put(id, action);
        return action;
    }


    public Optional<BaseAction> findById(String uuid) {
        return Optional.ofNullable(actions.get(uuid));
    }

    public List<BaseAction> findAllActionsOf(String portfolioId) {
        return actions.values().stream()
                .filter(a -> a.getPortfolio() != null && a.getPortfolio().getUuid().equals(portfolioId))
                .sorted(Comparator.comparing(BaseAction::getDatetime))
                .toList();
    }

    public void deleteById(String uuid) {
        if (uuid != null) actions.remove(uuid);
    }

    public List<BaseAction> findAllActionsOfUntilDate(String portfolioId, LocalDateTime untilDateTime) {
        return actions.values().stream()
                .filter(a -> a.getPortfolio() != null && a.getPortfolio().getUuid().equals(portfolioId))
                .filter(a -> a.getDatetime() != null && !a.getDatetime().isAfter(untilDateTime))
                .sorted(Comparator.comparing(BaseAction::getDatetime))
                .toList();
    }

    public void clear() {
        this.actions.clear();
    }
}
