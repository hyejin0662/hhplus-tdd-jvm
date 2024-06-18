package io.hhplus.tdd.point.dto.request;

import io.hhplus.tdd.point.type.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PointHistoryRequest {
    private long userId;
    private long amount;
    private TransactionType type;
}
