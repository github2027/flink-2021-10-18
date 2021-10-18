package com.atguigu.day07;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginEvent {
    private Long userId;
    private String ip;
    private String eventType;
    private Long eventTime;
}
