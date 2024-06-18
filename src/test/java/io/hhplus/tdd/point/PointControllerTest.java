package io.hhplus.tdd.point;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.hhplus.tdd.ApiControllerAdvice;

@ExtendWith(MockitoExtension.class)
class PointControllerTest {

	@Mock
	private PointService pointService;

	@InjectMocks
	private PointController pointController;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.standaloneSetup(pointController)
			.setControllerAdvice(new ApiControllerAdvice())
			.build();
	}

	/**
	 * 유저의 포인트 조회
	 */
	@Test
	void testGetUserPoint() throws Exception {
		long userId = 1L;
		UserPoint userPoint = new UserPoint(userId, 100, System.currentTimeMillis());
		when(pointService.getUserPoint(userId)).thenReturn(userPoint);

		mockMvc.perform(get("/point/{id}", userId))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"id\":1,\"point\":100,\"updateMillis\":" + userPoint.updateMillis() + "}"));
	}

	/**
	 * 유저의 포인트 내역 조회
	 */
	@Test
	void testGetPointHistories() throws Exception {
		long userId = 1L;
		PointHistory history1 = new PointHistory(1, userId, 50, TransactionType.CHARGE, System.currentTimeMillis());
		PointHistory history2 = new PointHistory(2, userId, -20, TransactionType.USE, System.currentTimeMillis());
		List<PointHistory> histories = List.of(history1, history2);
		when(pointService.getPointHistories(userId)).thenReturn(histories);

		mockMvc.perform(get("/point/{id}/histories", userId))
			.andExpect(status().isOk())
			.andExpect(content().json("[" +
				"{\"id\":1,\"userId\":1,\"amount\":50,\"type\":\"CHARGE\",\"updateMillis\":" + history1.updateMillis() + "}," +
				"{\"id\":2,\"userId\":1,\"amount\":-20,\"type\":\"USE\",\"updateMillis\":" + history2.updateMillis() + "}" +
				"]"));
	}

	/**
	 * 유저의 포인트 충전 테스트
	 */

	@Test
	void testChargePoint() throws Exception {
		long userId = 1L;
		long amount = 50;
		UserPoint userPoint = new UserPoint(userId, 150, System.currentTimeMillis());
		when(pointService.chargePoint(userId, amount)).thenReturn(userPoint);

		mockMvc.perform(patch("/point/{id}/charge", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.valueOf(amount)))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"id\":1,\"point\":150,\"updateMillis\":" + userPoint.updateMillis() + "}"));
	}

	/**
	 * 유저의 포인트 사용 테스트
	 */

	@Test
	void testUsePoint() throws Exception {
		long userId = 1L;
		long amount = 20;
		UserPoint userPoint = new UserPoint(userId, 80, System.currentTimeMillis());
		when(pointService.usePoint(userId, amount)).thenReturn(userPoint);

		mockMvc.perform(patch("/point/{id}/use", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.valueOf(amount)))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"id\":1,\"point\":80,\"updateMillis\":" + userPoint.updateMillis() + "}"));
	}

	/**
	 * 예외 상황 테스트
	 */
	@Test
	void testUsePointInsufficientBalance() throws Exception {
		long userId = 1L;
		long amount = 200;
		when(pointService.usePoint(userId, amount)).thenThrow(new IllegalArgumentException("포인트가 부족합니다."));

		mockMvc.perform(patch("/point/{id}/use", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.valueOf(amount)))
			.andExpect(status().isBadRequest())
			.andExpect(content().json("{\"code\":\"400\",\"message\":\"포인트가 부족합니다.\"}"));
	}
}
