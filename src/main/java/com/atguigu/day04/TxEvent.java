package com.atguigu.day04;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class TxEvent {
    private String txId;
    private String payChannel;
    private Long eventTime;

}
