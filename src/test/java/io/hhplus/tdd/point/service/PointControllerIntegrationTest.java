package io.hhplus.tdd.point.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.hhplus.tdd.point.controller.PointController;
import io.hhplus.tdd.point.dto.request.PointChargeRequest;
import io.hhplus.tdd.point.dto.request.PointUseRequest;
import io.hhplus.tdd.point.dto.response.PointChargeResponse;
import io.hhplus.tdd.point.dto.response.PointUseResponse;
import io.hhplus.tdd.point.type.TransactionType;

@SpringBootTest
@AutoConfigureMockMvc
public class PointControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PointService pointService;

    @Autowired
    private PointController pointController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(pointController)
            .build();
        objectMapper = new ObjectMapper();
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

        assertThat(pointService.getUserPoint(userId).getAmount()).isEqualTo( concurrentRequests * amount);
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
        long chargeAmount = 1000;
        long useAmount = 20;
        int concurrentRequests = 10;

        PointChargeRequest initialChargeRequest = new PointChargeRequest(userId, chargeAmount);
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialChargeRequest)))
            .andExpect(status().isOk());

        PointUseRequest useRequest = new PointUseRequest(userId, useAmount);
        PointUseResponse useResponse = new PointUseResponse(userId, -useAmount, TransactionType.USE, System.currentTimeMillis());

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);

        IntStream.range(0, concurrentRequests).forEach(i -> executorService.submit(() -> {
            try {
                latch.await();
                mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(useResponse)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        long expectedFinalAmount = chargeAmount - (concurrentRequests * useAmount);
        assertThat(pointService.getUserPoint(userId).getAmount()).isEqualTo(expectedFinalAmount);
    }
}
