package io.hhplus.tdd.point.service.impl;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.dto.request.PointChargeRequest;
import io.hhplus.tdd.point.dto.request.PointUseRequest;
import io.hhplus.tdd.point.dto.response.PointSearchResponse;
import io.hhplus.tdd.point.dto.response.PointUseResponse;
import io.hhplus.tdd.point.type.TransactionType;
import io.hhplus.tdd.point.entity.UserPoint;
import io.hhplus.tdd.point.dto.response.PointHistoryResponse;
import io.hhplus.tdd.point.dto.response.PointChargeResponse;
import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Override
    public PointSearchResponse getUserPoint(long id) {
        UserPoint userPoint = userPointTable.selectById(id);
        return new PointSearchResponse(userPoint.id(), userPoint.point(), userPoint.updateMillis());
    }

    @Override
    public List<PointHistoryResponse> getPointHistories(long id) {
        return pointHistoryTable.selectAllByUserId(id)
            .stream()
            .map(history -> new PointHistoryResponse( history.userId(), history.amount(), history.type(), history.updateMillis()))
            .collect(Collectors.toList());
    }


    @Override
    public synchronized PointChargeResponse  chargePoint(long id,PointChargeRequest request) {
        UserPoint userPoint = userPointTable.selectById(request.getId());
        userPoint = userPointTable.insertOrUpdate(request.getId(), userPoint.point() + request.getPoint());
        PointHistory history = pointHistoryTable.insert(request.getId(), request.getPoint(), TransactionType.CHARGE, System.currentTimeMillis());
        return new PointChargeResponse( history.userId(), history.amount(),TransactionType.CHARGE, history.updateMillis());
    }

    @Override
    public synchronized PointUseResponse usePoint(long id,PointUseRequest request) {
        UserPoint userPoint = userPointTable.selectById(request.getId());
        if (userPoint.point() < request.getPoint()) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        userPoint = userPointTable.insertOrUpdate(request.getId(), userPoint.point() - request.getPoint());
        PointHistory history = pointHistoryTable.insert(request.getId(), userPoint.point() - request.getPoint(), TransactionType.USE, System.currentTimeMillis());
        return new PointUseResponse(history.userId(), history.amount(), TransactionType.USE, history.updateMillis());
    }
}
