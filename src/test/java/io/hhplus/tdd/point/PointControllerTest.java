package io.hhplus.tdd.point;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.hhplus.tdd.point.controller.PointController;
import io.hhplus.tdd.point.dto.response.PointHistoryResponse;
import io.hhplus.tdd.point.dto.response.UserPointResponse;
import io.hhplus.tdd.point.error.ApiControllerAdvice;
import io.hhplus.tdd.point.service.PointService;
import io.hhplus.tdd.point.type.TransactionType;

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
		UserPointResponse userPointResponse = new UserPointResponse(userId, 100, System.currentTimeMillis());
		when(pointService.getUserPoint(userId)).thenReturn(userPointResponse);

		mockMvc.perform(get("/point/{id}", userId))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"id\":1,\"point\":100,\"updateMillis\":" + userPointResponse.getUpdateMillis() + "}"));
	}

	/**
	 * 유저의 포인트 내역 조회
	 */
	@Test
	void testGetPointHistories() throws Exception {
		long userId = 1L;
		PointHistoryResponse history1 = new PointHistoryResponse(1, userId, 50, TransactionType.CHARGE, System.currentTimeMillis());
		PointHistoryResponse history2 = new PointHistoryResponse(2, userId, -20, TransactionType.USE, System.currentTimeMillis());
		List<PointHistoryResponse> histories = List.of(history1, history2);
		when(pointService.getPointHistories(userId)).thenReturn(histories);

		mockMvc.perform(get("/point/{id}/histories", userId))
			.andExpect(status().isOk())
			.andExpect(content().json("[" +
				"{\"id\":1,\"userId\":1,\"amount\":50,\"type\":\"CHARGE\",\"updateMillis\":" + history1.getUpdateMillis() + "}," +
				"{\"id\":2,\"userId\":1,\"amount\":-20,\"type\":\"USE\",\"updateMillis\":" + history2.getUpdateMillis() + "}" +
				"]"));
	}

	/**
	 * 유저의 포인트 충전 테스트
	 */
	@Test
	void testChargePoint() throws Exception {
		long userId = 1L;
		long amount = 50;
		PointHistoryResponse pointHistoryResponse = new PointHistoryResponse(1, userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
		when(pointService.chargePoint(userId, amount)).thenReturn(pointHistoryResponse);

		mockMvc.perform(patch("/point/{id}/charge", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.valueOf(amount)))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"id\":1,\"userId\":1,\"amount\":50,\"type\":\"CHARGE\",\"updateMillis\":" + pointHistoryResponse.getUpdateMillis() + "}"));
	}

	/**
	 * 유저의 포인트 사용 테스트
	 */
	@Test
	void testUsePoint() throws Exception {
		long userId = 1L;
		long amount = 20;
		PointHistoryResponse pointHistoryResponse = new PointHistoryResponse(2, userId, -amount, TransactionType.USE, System.currentTimeMillis());
		when(pointService.usePoint(userId, amount)).thenReturn(pointHistoryResponse);

		mockMvc.perform(patch("/point/{id}/use", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.valueOf(amount)))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"id\":2,\"userId\":1,\"amount\":-20,\"type\":\"USE\",\"updateMillis\":" + pointHistoryResponse.getUpdateMillis() + "}"));
	}

	/**
	 * 예외 상황 테스트 1. 포인트 부족
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
	/**
	 * 예외 상황 테스트 2. 동시 다발적인 포인트 사용 시 잔고 부족
	 */
	@Test
	void testConcurrentUseWithInsufficientBalance() throws Exception {
		long userId = 1L;
		int threadCount = 10;
		long initialPoint = 100;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		List<Callable<Void>> tasks = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(1);

		for (int i = 0; i < threadCount; i++) {
			tasks.add(() -> {
				latch.await();
				mockMvc.perform(patch("/point/{id}/use", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("20"))
					.andExpect(status().isBadRequest())
					.andExpect(content().json("{\"code\":\"400\",\"message\":\"포인트가 부족합니다.\"}"));
				return null;
			});
		}

		UserPointResponse initialResponse = new UserPointResponse(userId, initialPoint, System.currentTimeMillis());
		when(pointService.getUserPoint(userId)).thenReturn(initialResponse);
		IntStream.range(0, threadCount).forEach(i -> {
			when(pointService.usePoint(userId, 20)).thenThrow(new IllegalArgumentException("포인트가 부족합니다."));
		});

		latch.countDown(); // 모든 스레드가 동시에 시작하도록 카운트 다운

		List<Future<Void>> futures = executorService.invokeAll(tasks);
		for (Future<Void> future : futures) {
			future.get();
		}

		executorService.shutdown();
	}

	/**
	 * 동시성 이슈 테스트 - 동시에 포인트 충전 및 사용 요청
	 */
	@Test
	void testConcurrentChargeAndUse() throws Exception {
		long userId = 1L;
		int threadCount = 10;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		List<Callable<Void>> tasks = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(1);

		for (int i = 0; i < threadCount / 2; i++) {
			tasks.add(() -> {
				latch.await();
				mockMvc.perform(patch("/point/{id}/charge", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("100"))
					.andExpect(status().isOk());
				return null;
			});
			tasks.add(() -> {
				latch.await();
				mockMvc.perform(patch("/point/{id}/use", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("50"))
					.andExpect(status().isOk());
				return null;
			});
		}

		UserPointResponse initialResponse = new UserPointResponse(userId, 0, System.currentTimeMillis());
		when(pointService.getUserPoint(userId)).thenReturn(initialResponse);

		for (int i = 0; i < threadCount / 2; i++) {
			PointHistoryResponse chargeResponse = new PointHistoryResponse(i, userId, 100, TransactionType.CHARGE, System.currentTimeMillis());
			PointHistoryResponse useResponse = new PointHistoryResponse(i + threadCount / 2, userId, -50, TransactionType.USE, System.currentTimeMillis());
			when(pointService.chargePoint(userId, 100)).thenReturn(chargeResponse);
			when(pointService.usePoint(userId, 50)).thenReturn(useResponse);
		}

		latch.countDown(); // 모든 스레드가 동시에 시작하도록 카운트 다운

		List<Future<Void>> futures = executorService.invokeAll(tasks);
		for (Future<Void> future : futures) {
			future.get();
		}

		executorService.shutdown();
	}



}
