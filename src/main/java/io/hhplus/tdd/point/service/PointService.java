package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.dto.response.PointHistoryResponse;
import io.hhplus.tdd.point.dto.response.UserPointResponse;

import java.util.List;

public interface PointService {
	UserPointResponse getUserPoint(long id);
	List<PointHistoryResponse> getPointHistories(long id);
	PointHistoryResponse chargePoint(long id, long amount);
	PointHistoryResponse usePoint(long id, long amount);
}
