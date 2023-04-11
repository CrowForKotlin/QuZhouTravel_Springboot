package com.app.yyqz.algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomUser {
  // 用户喜欢的美食
  private ArrayList<CustomFood> foods;
}
