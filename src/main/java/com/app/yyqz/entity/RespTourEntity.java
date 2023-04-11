package com.app.yyqz.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RespTourEntity {
    private int code;
    private String name;
    private String tickets;
    private String level;
    private String city;
    private String locate;
    private String openTime;
    private String desc;
    private String imageName;
}
