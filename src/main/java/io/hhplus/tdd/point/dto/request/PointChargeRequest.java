package io.hhplus.tdd.point.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PointChargeRequest {
    private long id;
    private long point;
}