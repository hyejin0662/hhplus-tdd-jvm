package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.stereotype.Service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {

	private final UserPointTable userPointTable;
	private final PointHistoryTable pointHistoryTable;


	public UserPoint getUserPoint(long id) {
		return userPointTable.selectById(id);
	}

	public List<PointHistory> getPointHistories(long id) {
		return pointHistoryTable.selectAllByUserId(id);
	}

	public synchronized UserPoint chargePoint(long id, long amount) {
		UserPoint userPoint = getUserPoint(id);
		userPoint = userPointTable.insertOrUpdate(id, userPoint.point() + amount);
		pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
		return userPoint;
	}

	public synchronized UserPoint usePoint(long id, long amount) {
		UserPoint userPoint = getUserPoint(id);
		if (userPoint.point() < amount) {
			throw new IllegalArgumentException("포인트가 부족합니다.");
		}
		userPoint = userPointTable.insertOrUpdate(id, userPoint.point() - amount);
		pointHistoryTable.insert(id, -amount, TransactionType.USE, System.currentTimeMillis());
		return userPoint;
	}
}