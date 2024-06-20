package io.hhplus.tdd.point.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.dto.request.PointChargeRequest;
import io.hhplus.tdd.point.dto.request.PointUseRequest;
import io.hhplus.tdd.point.dto.response.PointChargeResponse;
import io.hhplus.tdd.point.dto.response.PointHistoryResponse;
import io.hhplus.tdd.point.dto.response.PointSearchResponse;
import io.hhplus.tdd.point.dto.response.PointUseResponse;
import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.entity.UserPoint;
import io.hhplus.tdd.point.type.TransactionType;
import jdk.jfr.Description;

@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

	@Mock
	private UserPointTable userPointTable;

	@Mock
	private PointHistoryTable pointHistoryTable;

	@InjectMocks
	private PointServiceImpl pointService;

	private UserPoint userPoint;
	private PointChargeRequest chargeRequest;
	private PointUseRequest useRequest;

	@BeforeEach
	void setUp() {
		userPoint = new UserPoint(1L, 100L, System.currentTimeMillis());
		chargeRequest = new PointChargeRequest(1L, 50L);
		useRequest = new PointUseRequest(1L, 20L);
	}

	@Test
	@DisplayName("유저 포인트 조회 테스트")
	@Description("유저의 포인트를 조회하는 메서드의 테스트. 주어진 ID로 유저의 포인트 정보를 가져와서 예상된 값과 비교합니다.")
	void testGetUserPoint() {
		// given
		when(userPointTable.selectById(1L)).thenReturn(userPoint);

		// when
		PointSearchResponse response = pointService.getUserPoint(1L);

		// then
		assertNotNull(response);
		assertEquals(1L, response.getUserId());
		assertEquals(100L, response.getAmount());
	}

	@Test
	@DisplayName("유저 포인트 내역 조회 테스트")
	@Description("유저의 포인트 내역을 조회하는 메서드의 테스트. 주어진 ID로 유저의 포인트 내역을 가져와서 예상된 값과 비교합니다.")
	void testGetPointHistories() {
		// given
		PointHistory history1 = new PointHistory(1L, 1L, 50L, TransactionType.CHARGE, System.currentTimeMillis());
		PointHistory history2 = new PointHistory(2L, 1L, -20L, TransactionType.USE, System.currentTimeMillis());
		List<PointHistory> histories = List.of(history1, history2);

		when(pointHistoryTable.selectAllByUserId(1L)).thenReturn(histories);

		// when
		List<PointHistoryResponse> response = pointService.getPointHistories(1L);

		// then
		assertNotNull(response);
		assertEquals(2, response.size());
		assertEquals(50L, response.get(0).getAmount());
		assertEquals(-20L, response.get(1).getAmount());
	}

	@Test
	@DisplayName("포인트 충전 테스트")
	@Description("포인트를 충전하는 메서드의 테스트. 주어진 요청으로 포인트를 충전하고 결과를 검증합니다.")
	void testChargePoint() {
		// given
		when(userPointTable.selectById(1L)).thenReturn(userPoint);
		when(userPointTable.insertOrUpdate(1L, 150L)).thenReturn(new UserPoint(1L, 150L, System.currentTimeMillis()));
		when(pointHistoryTable.insert(anyLong(), anyLong(), any(TransactionType.class), anyLong()))
			.thenReturn(new PointHistory(1L, 1L, 50L, TransactionType.CHARGE, System.currentTimeMillis()));

		// when
		PointChargeResponse response = pointService.chargePoint(1L, chargeRequest);

		// then
		assertNotNull(response);
		assertEquals(1L, response.getUserId());
		assertEquals(50L, response.getAmount());
		assertEquals(TransactionType.CHARGE, response.getType());
	}

	@Test
	@DisplayName("포인트 사용 테스트")
	@Description("포인트를 사용하는 메서드의 테스트. 주어진 요청으로 포인트를 사용하고 결과를 검증합니다.")
	void testUsePoint() {
		// given
		when(userPointTable.selectById(1L)).thenReturn(userPoint);
		when(userPointTable.insertOrUpdate(1L, 80L)).thenReturn(new UserPoint(1L, 80L, System.currentTimeMillis()));
		when(pointHistoryTable.insert(anyLong(), anyLong(), any(TransactionType.class), anyLong()))
			.thenReturn(new PointHistory(1L, 1L, -20L, TransactionType.USE, System.currentTimeMillis()));

		// when
		PointUseResponse response = pointService.usePoint(1L, useRequest);

		// then
		assertNotNull(response);
		assertEquals(1L, response.getUserId());
		assertEquals(-20L, response.getAmount());
		assertEquals(TransactionType.USE, response.getType());
	}

	@Test
	@DisplayName("포인트 부족 예외 테스트")
	@Description("포인트 사용 시 잔액 부족으로 인해 예외가 발생하는 경우를 테스트. 주어진 요청으로 포인트를 사용하고 잔액이 부족할 때 예외를 검증합니다.")
	void testUsePointInsufficientBalance() {
		// given
		when(userPointTable.selectById(1L)).thenReturn(userPoint);

		PointUseRequest request = new PointUseRequest(1L, 200L);

		// when
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			pointService.usePoint(1L, request);
		});

		// then
		assertEquals("포인트가 부족합니다.", exception.getMessage());
	}

	@Test
	@DisplayName("포인트 충전 시 유효하지 않은 값 예외 테스트")
	@Description("포인트 충전 시 음수 값을 입력하면 예외가 발생하는 경우를 테스트. 유효하지 않은 요청으로 포인트를 충전할 때 예외를 검증합니다.")
	void testChargeNegativeAmount() {
		// given
		PointChargeRequest request = new PointChargeRequest(1L, -50L);

		// when
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			pointService.chargePoint(1L, request);
		});

		// then
		assertEquals("유효하지 않은 충전 값입니다.", exception.getMessage());
	}

	@Test
	@DisplayName("포인트 사용 시 유효하지 않은 값 예외 테스트")
	@Description("포인트 사용 시 음수 값을 입력하면 예외가 발생하는 경우를 테스트. 유효하지 않은 요청으로 포인트를 사용할 때 예외를 검증합니다.")
	void testUseNegativeAmount() {
		// given
		PointUseRequest request = new PointUseRequest(1L, -20L);

		// when
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			pointService.usePoint(1L, request);
		});

		// then
		assertEquals("유효하지 않은 사용 값입니다.", exception.getMessage());
	}
}
