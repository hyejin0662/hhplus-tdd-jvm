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



}
