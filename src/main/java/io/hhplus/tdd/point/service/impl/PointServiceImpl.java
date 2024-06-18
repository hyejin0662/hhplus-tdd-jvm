package io.hhplus.tdd.point.service.impl;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.type.TransactionType;
import io.hhplus.tdd.point.entity.UserPoint;
import io.hhplus.tdd.point.dto.response.PointHistoryResponse;
import io.hhplus.tdd.point.dto.response.UserPointResponse;
import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Override
    public UserPointResponse getUserPoint(long id) {
        UserPoint userPoint = userPointTable.selectById(id);
        return new UserPointResponse(userPoint.id(), userPoint.point(), userPoint.updateMillis());
    }

    @Override
    public List<PointHistoryResponse> getPointHistories(long id) {
        return pointHistoryTable.selectAllByUserId(id)
            .stream()
            .map(history -> new PointHistoryResponse(history.id(), history.userId(), history.amount(), history.type(), history.updateMillis()))
            .collect(Collectors.toList());
    }

    @Override
    public synchronized PointHistoryResponse chargePoint(long id, long amount) {
        UserPoint userPoint = userPointTable.selectById(id);
        userPoint = userPointTable.insertOrUpdate(id, userPoint.point() + amount);
        PointHistory pointHistory = pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
        return new PointHistoryResponse(pointHistory.id(), pointHistory.userId(), pointHistory.amount(), pointHistory.type(), pointHistory.updateMillis());
    }

    @Override
    public synchronized PointHistoryResponse usePoint(long id, long amount) {
        UserPoint userPoint = userPointTable.selectById(id);
        if (userPoint.point() < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        userPoint = userPointTable.insertOrUpdate(id, userPoint.point() - amount);
        PointHistory pointHistory = pointHistoryTable.insert(id, -amount, TransactionType.USE, System.currentTimeMillis());
        return new PointHistoryResponse(pointHistory.id(), pointHistory.userId(), pointHistory.amount(), pointHistory.type(), pointHistory.updateMillis());
    }
}
