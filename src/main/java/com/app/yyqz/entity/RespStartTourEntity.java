package com.app.yyqz.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RespStartTourEntity {
    private String name;
    private Boolean isStart;
}
