package io.hhplus.tdd.point;

import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PointControllerTest {

	@Mock
	private PointService pointService;

	@InjectMocks
	private PointController pointController;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(pointController).build();
	}

	@Test
	void testGetUserPoint() throws Exception {
		long userId = 1L;
		UserPoint userPoint = new UserPoint(userId, 100, System.currentTimeMillis());
		when(pointService.getUserPoint(userId)).thenReturn(userPoint);

		mockMvc.perform(get("/point/{id}", userId))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"id\":1,\"point\":100,\"updateMillis\":" + userPoint.updateMillis() + "}"));
	}

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
}
