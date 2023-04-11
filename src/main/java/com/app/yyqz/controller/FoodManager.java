package com.app.yyqz.controller;

import com.app.yyqz.algorithm.FoodScore;
import com.app.yyqz.entity.FoodEntity;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.val;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unchecked")
@NoArgsConstructor
@AllArgsConstructor
public class FoodManager {

    public Gson gson;
    public Type foodScoreType;

    public void GetAllUserFoodScore(JdbcTemplate jdbc, String usr, AtomicReference<Map<String, Map<String, Integer>>> otherUserScoreDatas, AtomicReference<ArrayList<FoodScore>> userSelfScoreDatas ) {
        jdbc.query("select yyqz.yyqz.UserName, yyqz.yyqz.score from yyqz.yyqz", rs -> {
            GetUserFoodScore(rs, usr, otherUserScoreDatas, userSelfScoreDatas);
            while (rs.next()) {
                GetUserFoodScore(rs, usr, otherUserScoreDatas, userSelfScoreDatas);
            }
        });
    }
    public void GetAllFood(JdbcTemplate jdbc, AtomicReference<ArrayList<FoodEntity>> respFoodResult, HttpServletResponse resp) {
        // 查询全部美食
        jdbc.query("select * from yyqz.food", rs -> {

            // 得到美食和评分就可以通过 respFoodResult拿取
            doFoodBuildRespResult(rs, respFoodResult);

            // 光标移动到下一个
            while (rs.next()) {
                doFoodBuildRespResult(rs, respFoodResult);
            }

            resp.setStatus(HttpServletResponse.SC_OK);
        });
    }
    public ArrayList<FoodEntity> GetFoodRecommendResultBySort(AtomicReference<List<Map<String,Integer>>> recommendations, AtomicReference<ArrayList<FoodEntity>> respFoodResult, AtomicReference<ArrayList<FoodScore>> userSelfScoreDatas) {
        ArrayList<FoodEntity> foodEntities = (ArrayList<FoodEntity>) respFoodResult.get().clone();


        // 如果获取自己用户的评分数据为空
        if (!userSelfScoreDatas.get().isEmpty()) {
            for (FoodEntity foodEntity : respFoodResult.get()) {
                for (FoodScore food : userSelfScoreDatas.get()) {
                    if (foodEntity.getTitle().contentEquals(food.getFoodName())) {
                        foodEntities.remove(foodEntity);
                        foodEntity.setScore(food.getScore());
                        foodEntities.add(0, foodEntity);
                    }
                }
            }
        }

        if (!recommendations.get().isEmpty()) {

            for (Map<String, Integer> integerMap : recommendations.get()) {
                for (Map.Entry<String, Integer> entry : integerMap.entrySet()) {
                    for (FoodEntity food : respFoodResult.get()) {
                        if (entry.getKey().contentEquals(food.getTitle())) {
                            foodEntities.remove(food);
                            food.setScore(entry.getValue());
                            foodEntities.add(0, food);
                        }
                    }
                }
            }
            return foodEntities;
        }
        return foodEntities;
    }
    public ArrayList<FoodEntity> GetFoodLikeTypeResultBySort(AtomicReference<String> likeType, AtomicReference<ArrayList<FoodEntity>> respFoodResult, AtomicReference<ArrayList<FoodScore>> userSelfScoreDatas) {

        ArrayList<FoodEntity> foodEntities = (ArrayList<FoodEntity>) respFoodResult.get().clone();

        for (FoodEntity foodEntity : respFoodResult.get()) {
            for (FoodScore food : userSelfScoreDatas.get()) {
                if (foodEntity.getTitle().contentEquals(food.getFoodName())) {
                    foodEntities.remove(foodEntity);
                    foodEntity.setScore(food.getScore());
                    foodEntities.add(0, foodEntity);
                }
            }
        }

        String like = likeType.get();
        if (!like.isEmpty()) {
            val foodResult = (ArrayList<FoodEntity>) foodEntities.clone();
            for (FoodEntity food : respFoodResult.get()) {
                if (like.contains(String.valueOf(food.getType()))) {
                    foodResult.remove(food);
                    foodResult.add(0, food);
                }
            }
            return foodResult;
        }


        return foodEntities;
    }


    private void GetUserFoodScore(ResultSet rs, String usr, AtomicReference<Map<String, Map<String, Integer>>> otherUserScoreDatas, AtomicReference<ArrayList<FoodScore>> userSelfScoreDatas) {
        try {
            // 获取用户名
            var accountUsr = rs.getString(1);

            // 获取评分数据
            var score = rs.getString(2);

            // 如果评分数据为空或者不存在 就退出
            if (score == null || score.isEmpty()) {
                // userSelfScoreDatas.set(new ArrayList<>());
                return;
            }


            // 设置
            if (otherUserScoreDatas != null) otherUserScoreDatas.set(doPreData(otherUserScoreDatas, accountUsr, score));
            if (accountUsr.equals(usr)) {
                userSelfScoreDatas.set(gson.fromJson(score, this.foodScoreType));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private Map<String, Map<String, Integer>> doPreData(AtomicReference<Map<String, Map<String, Integer>>> userData, String username, String score) {
        Map<String, Integer> foods = new HashMap<>();
        ArrayList<FoodScore> foodScores = gson.fromJson(score, this.foodScoreType);
        if (foodScores == null || foodScores.isEmpty()) {
            userData.set(new HashMap<>());
            return userData.get();
        }
        for (FoodScore foodScore : foodScores) {
            foods.put(foodScore.getFoodName(), foodScore.getScore());
        }
        userData.get().put(username, foods);
        return userData.get();
    }
    private void doFoodBuildRespResult(ResultSet rs, AtomicReference<ArrayList<FoodEntity>> respFoodResult) {
        try {
            respFoodResult.get().add(new FoodEntity(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getInt(5),
                    rs.getString(6),
                    0
            ));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
