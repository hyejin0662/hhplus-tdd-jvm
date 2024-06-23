package io.hhplus.tdd.point.dto.response;

import io.hhplus.tdd.point.type.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PointSearchResponse {

	private long userId;
	private long amount;
	private long updateMillis;
}
