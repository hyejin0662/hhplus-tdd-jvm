package io.hhplus.tdd.point.repository;

import java.util.List;

import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.type.TransactionType;

public interface PointHistoryRepository {
	PointHistory insert(long userId, long amount, TransactionType type, long updateMillis);
	List<PointHistory> selectAllByUserId(long userId);
}