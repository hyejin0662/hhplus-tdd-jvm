package io.hhplus.tdd.point.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.type.TransactionType;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

	private final PointHistoryTable pointHistoryTable;

	@Override
	public PointHistory insert(long userId, long amount, TransactionType type, long updateMillis) {
		return pointHistoryTable.insert(userId, amount, type, updateMillis);
	}

	@Override
	public List<PointHistory> selectAllByUserId(long userId) {
		return pointHistoryTable.selectAllByUserId(userId);
	}
}
