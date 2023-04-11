package com.app.yyqz.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespFoodEntity {
    private int code;
    private ArrayList<FoodEntity> foodEntities;
}
