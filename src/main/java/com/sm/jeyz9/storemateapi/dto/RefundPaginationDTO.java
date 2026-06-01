package com.sm.jeyz9.storemateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundPaginationDTO {
    private List<RefundDTO> refunds;
    private int pendingCount;
    private Integer page;
    private Integer size;
    private int total;
}
