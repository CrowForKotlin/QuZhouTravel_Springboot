package com.app.yyqz.algorithm;

import java.util.*;

// 定义一个类来执行协同过滤推荐
public class CollaborativeFilteringRecommender {

    // 最多推荐的美食数量
    static public int K = 3;

    // 定义一个Map来存储用户和美食的评分数据
    Map<String, Map<String, Integer>> userFoodScores;

    // 定义一个构造函数来初始化用户和美食的评分数据
    public CollaborativeFilteringRecommender(Map<String, Map<String, Integer>> userFoodScores) {
        this.userFoodScores = userFoodScores;
    }

    // 定义一个方法来获取推荐列表 （不管其他用户评分如何，只会推荐用户没选过的）
    public List<Map<String,Integer>>  getRecommendationsForUser(String userId) {

        // 创建一个List来存储推荐结果
        List<Map<String,Integer>> recommendations = new ArrayList<>();

        // 获取指定用户的评分数据
        Map<String, Integer> userScores = userFoodScores.get(userId);

        // 遍历所有用户的评分数据，计算与指定用户的相似度
        for (Map.Entry<String, Map<String, Integer>> entry : userFoodScores.entrySet()) {
            // 获取当前用户的ID
            String otherUserId = entry.getKey();

            // 获取当前用户的评分数据
            Map<String, Integer> otherUserScores = entry.getValue();

            // 如果当前用户与指定用户相同，则跳过
            if (userId.equals(otherUserId)) {
                continue;
            }

            // 计算两个用户的余弦相似度
            double similarity = calculateSimilarity(userScores, otherUserScores);

            HashMap<String,Integer> recommendationMap = new HashMap<>();

            // 如果两个用户的相似度大于0，则认为他们是相似的，并推荐当前用户未评分的美食
            if (similarity > 0) {
                // 遍历当前用户的评分数据，获取当前用户未评分的美食
                for (Map.Entry<String, Integer> scoreEntry : otherUserScores.entrySet()) {
                    // 获取美食的ID
                    String foodId = scoreEntry.getKey();

                    // 获取美食的评分
                    int score = scoreEntry.getValue();

                    // 如果指定用户未评分该美食，则添加到推荐列表中
                    if (!userScores.containsKey(foodId)) {
                        recommendationMap.put(foodId, score);
                        recommendations.add(recommendationMap);
                    }
                }
            }
        }

        // 返回推荐列表
        return recommendations;
    }

    // 定义一个方法来计算两个用户之间的相似度
    private double calculateSimilarity(Map<String, Integer> userScores, Map<String, Integer> otherUserScores) {

        // 定义两个变量来存储向量的乘积和两个向量的模长的乘积
        double product = 0;
        double magnitudeA = 0;
        double magnitudeB = 0;

        // 遍历指定用户的评分数据
        for (Map.Entry<String, Integer> entry : userScores.entrySet()) {
            // 获取美食的ID
            String foodId = entry.getKey();

            // 获取美食的评分
            int score = entry.getValue();

            // 如果当前用户也评分了该美食，则计算向量的乘积和模长的平方
            if (otherUserScores.containsKey(foodId)) {
                // 获取当前用户对该美食的评分
                int otherScore = otherUserScores.get(foodId);

                // 计算向量的乘积
                product += score * otherScore;

                // 计算指定用户评分的美食的模长的平方
                magnitudeA += score * score;

                // 计算当前用户评分的美食的模长的平方
                magnitudeB += otherScore * otherScore;
            }
        }

        // 如果两个向量的模长的乘积为0，则相似度为0
        if (magnitudeA * magnitudeB == 0) {
            return 0;
        }

        // 计算两个用户的余弦相似度
        double similarity = product / Math.sqrt(magnitudeA * magnitudeB);

        // 返回相似度
        return similarity;
    }

    public static void main(String[] args) {

        // 定义一个Map来存储用户和美食的评分数据
        Map<String, Map<String, Integer>> userFoodScores = new HashMap<>();

        // 添加用户1的评分数据
        Map<String, Integer> user1Scores = new HashMap<>();
        user1Scores.put("food1", 5);
        user1Scores.put("food2", 5);
        user1Scores.put("food3", 4);
        user1Scores.put("food4", 2);
        userFoodScores.put("user1", user1Scores);

        // 添加用户2的评分数据
        Map<String, Integer> user2Scores = new HashMap<>();
        user2Scores.put("food1", 5);
        user2Scores.put("food2", 5);
        user2Scores.put("food5", 4);
        user2Scores.put("food6", 4);
        userFoodScores.put("user2", user2Scores);

        // 添加用户3的评分数据
        Map<String, Integer> user3Scores = new HashMap<>();
        user3Scores.put("food1", 5);
        user3Scores.put("food2", 5);
        user3Scores.put("food5", 0);
        user3Scores.put("food6", 4);
        userFoodScores.put("user3", user3Scores);

        // 创建协同过滤推荐器
        CollaborativeFilteringRecommender recommender = new CollaborativeFilteringRecommender(userFoodScores);

        // 获取推荐列表
        List<Map<String,Integer>> recommendations = recommender.getRecommendationsForUser("user1");

        // 打印推荐列表
        System.out.println(recommendations);
    }

}
