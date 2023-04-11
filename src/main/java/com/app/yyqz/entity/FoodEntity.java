package com.app.yyqz.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodEntity {

    private int id;
    private String title;
    private String desc;
    private String imageName;
    private int type;
    private String aid;
    private int score;
}
