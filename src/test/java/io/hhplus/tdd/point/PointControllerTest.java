package io.hhplus.tdd.point;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.hhplus.tdd.point.controller.PointController;
import io.hhplus.tdd.point.dto.request.PointChargeRequest;
import io.hhplus.tdd.point.dto.request.PointUseRequest;
import io.hhplus.tdd.point.dto.response.PointChargeResponse;
import io.hhplus.tdd.point.dto.response.PointHistoryResponse;
import io.hhplus.tdd.point.dto.response.PointSearchResponse;
import io.hhplus.tdd.point.dto.response.PointUseResponse;
import io.hhplus.tdd.point.error.ApiControllerAdvice;
import io.hhplus.tdd.point.error.ErrorResponse;
import io.hhplus.tdd.point.service.PointService;
import io.hhplus.tdd.point.type.TransactionType;

@ExtendWith(MockitoExtension.class)
class PointControllerTest {

	@Mock
	private PointService pointService;

	@InjectMocks
	private PointController pointController;

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.standaloneSetup(pointController)
			.setControllerAdvice(new ApiControllerAdvice())
			.build();
		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("유저의 포인트 단건 조회 테스트: 유저 ID로 포인트를 조회하는 테스트입니다.")
	void testGetUserPoint() throws Exception {
		long userId = 1L;
		PointSearchResponse pointSearchResponse = new PointSearchResponse(userId, 100, System.currentTimeMillis());

		// given
		when(pointService.getUserPoint(userId)).thenReturn(pointSearchResponse);

		// when
		mockMvc.perform(get("/point/{id}", userId))
			// then
			.andExpect(status().isOk())
			.andExpect(content().json(objectMapper.writeValueAsString(pointSearchResponse)));
	}

	@Test
	@DisplayName("유저의 포인트 내역 조회 테스트: 유저 ID로 포인트 내역을 조회하는 테스트입니다.")
	void testGetPointHistories() throws Exception {
		long userId = 1L;
		PointHistoryResponse history1 = new PointHistoryResponse(userId, 50, TransactionType.CHARGE, System.currentTimeMillis());
		PointHistoryResponse history2 = new PointHistoryResponse(userId, -20, TransactionType.USE, System.currentTimeMillis());
		List<PointHistoryResponse> histories = List.of(history1, history2);

		// given
		when(pointService.getPointHistories(userId)).thenReturn(histories);

		// when
		mockMvc.perform(get("/point/{id}/histories", userId))
			// then
			.andExpect(status().isOk())
			.andExpect(content().json(objectMapper.writeValueAsString(histories)));
	}

	@Test
	@DisplayName("유저의 포인트 충전 테스트: 유저 ID로 포인트를 충전하는 테스트입니다.")
	void testChargePoint() throws Exception {
		long userId = 1L;
		long amount = 50;
		PointChargeRequest request = new PointChargeRequest(userId, amount);

		PointChargeResponse pointHistoryResponse = new PointChargeResponse(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

		// given
		when(pointService.chargePoint(userId, request)).thenReturn(pointHistoryResponse);

		// when
		mockMvc.perform(patch("/point/{id}/charge", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			// then
			.andExpect(status().isOk())
			.andExpect(content().json(objectMapper.writeValueAsString(pointHistoryResponse)));
	}

	@Test
	@DisplayName("유저의 포인트 사용 테스트: 유저 ID로 포인트를 사용하는 테스트입니다.")
	void testUsePoint() throws Exception {
		long userId = 1L;
		long amount = 20;
		PointUseRequest request = new PointUseRequest(userId, amount);

		PointUseResponse pointUseResponse = PointUseResponse.builder()
			.userId(userId)
			.amount(-amount)
			.type(TransactionType.USE)
			.updateMillis(System.currentTimeMillis())
			.build();

		// given
		when(pointService.usePoint(userId, request)).thenReturn(pointUseResponse);

		// when
		mockMvc.perform(patch("/point/{id}/use", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			// then
			.andExpect(status().isOk())
			.andExpect(content().json(objectMapper.writeValueAsString(pointUseResponse)));
	}

	@Test
	@DisplayName("포인트 부족 예외 테스트: 유저가 사용할 포인트가 부족할 때의 예외 처리 테스트입니다.")
	public void testUsePointInsufficientBalance() throws Exception {
		long userId = 1L;
		PointUseRequest request = new PointUseRequest(userId, 200);

		// given
		doThrow(new IllegalArgumentException("포인트가 부족합니다."))
			.when(pointService)
			.usePoint(userId, request);

		// when
		mockMvc.perform(patch("/point/{id}/use", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"point\":200}"))
			// then
			.andExpect(status().isBadRequest());
	}


	@Test
	@DisplayName("예외 상황 테스트: 음수를 충전할 때의 예외 처리 테스트입니다.")
	void testChargeNegativeAmount() throws Exception {
		long userId = 1L;
		long amount = -50;
		PointChargeRequest request = new PointChargeRequest(userId, amount);

		// when
		mockMvc.perform(patch("/point/{id}/charge", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			// then
			.andExpect(status().isBadRequest())
			.andExpect(content().json(objectMapper.writeValueAsString(new ErrorResponse("400", "유효하지 않은 충전 값입니다."))));
	}

	@Test
	@DisplayName("예외 상황 테스트: 문자열을 충전할 때의 예외 처리 테스트입니다.")
	void testChargeInvalidStringAmount() throws Exception {
		long userId = 1L;
		String invalidAmount = "invalid";

		// when
		mockMvc.perform(patch("/point/{id}/charge", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"point\":\"" + invalidAmount + "\"}"))
			// then
			.andExpect(status().isBadRequest());
	}


	/**
	 * 동시성 제어 테스트: 여러 요청이 동시에 포인트를 충전하는 경우
	 * <p>
	 * 이 테스트는 다수의 요청이 동시에 포인트 충전 API를 호출할 때 동시성 문제가 발생하지 않는지 확인하는 것을 목적으로 합니다.
	 * <p>
	 * 1. 주어진 유저 ID와 포인트 금액으로 충전 요청을 만듭니다.
	 * 2. {@link ExecutorService}를 이용해 여러 스레드를 생성하고, 각 스레드가 동일한 충전 요청을 수행하도록 합니다.
	 * 3. {@link CountDownLatch}를 사용해 모든 스레드가 준비되기 전까지 대기하게 한 후, 동시에 실행되도록 합니다.
	 * 4. 모든 요청이 성공적으로 처리되었는지 확인하고, 서비스 메서드가 예상된 횟수만큼 호출되었는지 검증합니다.
	 *
	 * @throws Exception 예외가 발생할 경우
	 */
	@Test
	@DisplayName("동시성 제어 테스트: 여러 요청이 동시에 포인트를 충전하는 경우")
	void testConcurrentChargePoints() throws Exception {
		long userId = 1L;
		long amount = 50;
		int concurrentRequests = 10;

		PointChargeRequest request = new PointChargeRequest(userId, amount);
		PointChargeResponse response = new PointChargeResponse(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

		// given
		when(pointService.chargePoint(eq(userId), any(PointChargeRequest.class))).thenReturn(response);


		CountDownLatch latch = new CountDownLatch(1);
		ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);


		IntStream.range(0, concurrentRequests).forEach(i -> executorService.submit(() -> {
			try {
				latch.await();
				mockMvc.perform(patch("/point/{id}/charge", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(content().json(objectMapper.writeValueAsString(response)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));


		latch.countDown();


		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);


		verify(pointService, times(concurrentRequests)).chargePoint(eq(userId), any(PointChargeRequest.class));
	}

	/**
	 * 동시성 제어 테스트: 여러 요청이 동시에 포인트를 사용하는 경우
	 * <p>
	 * 이 테스트는 다수의 요청이 동시에 포인트 사용 API를 호출할 때 동시성 문제가 발생하지 않는지 확인하는 것을 목적으로 합니다.
	 * <p>
	 * 1. 주어진 유저 ID와 포인트 금액으로 사용 요청을 만듭니다.
	 * 2. {@link ExecutorService}를 이용해 여러 스레드를 생성하고, 각 스레드가 동일한 사용 요청을 수행하도록 합니다.
	 * 3. {@link CountDownLatch}를 사용해 모든 스레드가 준비되기 전까지 대기하게 한 후, 동시에 실행되도록 합니다.
	 * 4. 모든 요청이 성공적으로 처리되었는지 확인하고, 서비스 메서드가 예상된 횟수만큼 호출되었는지 검증합니다.
	 *
	 * @throws Exception 예외가 발생할 경우
	 */
	@Test
	@DisplayName("동시성 제어 테스트: 여러 요청이 동시에 포인트를 사용하는 경우")
	void testConcurrentUsePoints() throws Exception {
		long userId = 1L;
		long amount = 20;
		int concurrentRequests = 10;

		PointUseRequest request = new PointUseRequest(userId, amount);
		PointUseResponse response = new PointUseResponse(userId, -amount, TransactionType.USE, System.currentTimeMillis());

		// given
		when(pointService.usePoint(eq(userId), any(PointUseRequest.class))).thenReturn(response);


		CountDownLatch latch = new CountDownLatch(1);
		ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);


		IntStream.range(0, concurrentRequests).forEach(i -> executorService.submit(() -> {
			try {
				latch.await();
				mockMvc.perform(patch("/point/{id}/use", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(content().json(objectMapper.writeValueAsString(response)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));


		latch.countDown();


		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);


		verify(pointService, times(concurrentRequests)).usePoint(eq(userId), any(PointUseRequest.class));
	}




}
