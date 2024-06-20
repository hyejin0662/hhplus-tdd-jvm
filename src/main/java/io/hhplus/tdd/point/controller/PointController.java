package io.hhplus.tdd.point.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.hhplus.tdd.point.dto.request.PointChargeRequest;
import io.hhplus.tdd.point.dto.request.PointUseRequest;
import io.hhplus.tdd.point.dto.response.PointHistoryResponse;
import io.hhplus.tdd.point.dto.response.PointChargeResponse;
import io.hhplus.tdd.point.dto.response.PointSearchResponse;
import io.hhplus.tdd.point.dto.response.PointUseResponse;
import io.hhplus.tdd.point.service.PointService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;



    @GetMapping("{id}")
    public PointSearchResponse point(@PathVariable long id) {
        return pointService.getUserPoint(id);
    }



    @GetMapping("{id}/histories")
    public List<PointHistoryResponse> history(@PathVariable long id) {
        return pointService.getPointHistories(id);
    }



    @PatchMapping("{id}/charge")
    public PointChargeResponse charge(@PathVariable long id, @RequestBody PointChargeRequest request) {
        if (request.getPoint() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 충전 값입니다.");
        }
        return pointService.chargePoint(id, request);
    }



    @PatchMapping("{id}/use")
    public PointUseResponse use(@PathVariable long id, @RequestBody PointUseRequest request) {
        if (request.getPoint() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용 값입니다.");
        }
        return pointService.usePoint(id, request);
    }
}


