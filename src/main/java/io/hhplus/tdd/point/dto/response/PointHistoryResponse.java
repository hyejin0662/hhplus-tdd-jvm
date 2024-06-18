package io.hhplus.tdd.point.dto.response;

import io.hhplus.tdd.point.type.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PointHistoryResponse {
    private long id;
    private long userId;
    private long amount;
    private TransactionType type;
    private long updateMillis;
}
