package com.app.yyqz.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RespLoginEntity {
    private int code;
    private String name;
    private String username;
    private String password;
    private String likeType;
}
