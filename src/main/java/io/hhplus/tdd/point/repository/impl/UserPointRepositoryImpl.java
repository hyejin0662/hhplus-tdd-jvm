package io.hhplus.tdd.point.repository.impl;

import org.springframework.stereotype.Repository;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.entity.UserPoint;
import io.hhplus.tdd.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserPointRepositoryImpl implements UserPointRepository {
	private final UserPointTable userPointTable;

	@Override
	public UserPoint selectById(Long id) {
		return userPointTable.selectById(id);
	}

	@Override
	public UserPoint insertOrUpdate(long id, long amount) {
		return userPointTable.insertOrUpdate(id, amount);
	}
}