package io.hhplus.tdd.point.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPointResponse {
    private long id;
    private long point;
    private long updateMillis;
}