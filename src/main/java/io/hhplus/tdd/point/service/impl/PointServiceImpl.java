package io.hhplus.tdd.point.service.impl;

import io.hhplus.tdd.point.dto.request.PointChargeRequest;
import io.hhplus.tdd.point.dto.request.PointUseRequest;
import io.hhplus.tdd.point.dto.response.PointSearchResponse;
import io.hhplus.tdd.point.dto.response.PointUseResponse;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.type.TransactionType;
import io.hhplus.tdd.point.entity.UserPoint;
import io.hhplus.tdd.point.dto.response.PointHistoryResponse;
import io.hhplus.tdd.point.dto.response.PointChargeResponse;
import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Override
    public PointSearchResponse getUserPoint(long id) {
        UserPoint userPoint = userPointRepository.selectById(id);
        return new PointSearchResponse(userPoint.id(), userPoint.point(), userPoint.updateMillis());
    }

    @Override
    public List<PointHistoryResponse> getPointHistories(long id) {
        return pointHistoryRepository.selectAllByUserId(id)
            .stream()
            .map(history -> new PointHistoryResponse( history.userId(), history.amount(), history.type(), history.updateMillis()))
            .collect(Collectors.toList());
    }


    @Override
    public synchronized PointChargeResponse chargePoint(long id, PointChargeRequest request) {
        if (request.getPoint() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 충전 값입니다.");
        }
        UserPoint userPoint = userPointRepository.selectById(request.getId());
        userPoint = userPointRepository.insertOrUpdate(request.getId(), userPoint.point() + request.getPoint());
        PointHistory history = pointHistoryRepository.insert(request.getId(), request.getPoint(), TransactionType.CHARGE, System.currentTimeMillis());
        return new PointChargeResponse(history.userId(), history.amount(), TransactionType.CHARGE, history.updateMillis());
    }

    @Override
    public synchronized PointUseResponse usePoint(long id, PointUseRequest request) {
        if (request.getPoint() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용 값입니다.");
        }
        UserPoint userPoint = userPointRepository.selectById(request.getId());
        if (userPoint.point() < request.getPoint()) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        userPoint = userPointRepository.insertOrUpdate(request.getId(), userPoint.point() - request.getPoint());
        PointHistory history = pointHistoryRepository.insert(request.getId(), request.getPoint(), TransactionType.USE, System.currentTimeMillis());
        return new PointUseResponse(history.userId(), history.amount(), TransactionType.USE, history.updateMillis());
    }
}
