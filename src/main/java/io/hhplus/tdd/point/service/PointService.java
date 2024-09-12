package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.dto.request.PointChargeRequest;
import io.hhplus.tdd.point.dto.request.PointUseRequest;
import io.hhplus.tdd.point.dto.response.PointHistoryResponse;
import io.hhplus.tdd.point.dto.response.PointChargeResponse;
import io.hhplus.tdd.point.dto.response.PointSearchResponse;
import io.hhplus.tdd.point.dto.response.PointUseResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface PointService {
	PointSearchResponse getUserPoint(long id);
	List<PointHistoryResponse> getPointHistories(long id);
	PointChargeResponse chargePoint(long id, PointChargeRequest pointChargeRequest);
	PointUseResponse usePoint(long id, PointUseRequest pointUseRequest);
}
