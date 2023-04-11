package com.app.yyqz.controller;

import com.app.yyqz.algorithm.CollaborativeFilteringRecommender;
import com.app.yyqz.algorithm.FoodScore;
import com.app.yyqz.entity.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unchecked")
@RestController
public class UserController {

    // SpringBoot JDBC 封装API
    @Resource
    private JdbcTemplate jdbc;

    // 序列化工具
    private final Gson gson = new Gson();

    // 定义两个Type类型，方便Gson后续反序列化直接传参
    private final Type type = new TypeToken<ArrayList<UserTicketsDatas>>() {
    }.getType();
    private final Type foodScoreType = new TypeToken<ArrayList<FoodScore>>() {
    }.getType();

    // 日期格式
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // 管理美食一个
    private final FoodManager foodManager = new FoodManager(gson, foodScoreType);

    // 登录接口
    @PostMapping("/login")
    public RespLoginEntity postLogin(@RequestParam("username") String usr, @RequestParam("password") String pwd, HttpServletResponse resp) {
        // 先设置 请求失败的标志
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        // 如果查询到了数据 则请求成功
        AtomicReference<RespLoginEntity> respLoginEntity = new AtomicReference<>();
        jdbc.query("select * from yyqz.yyqz where UserName = '" + usr + "' and Password = '" + pwd + "' limit 1", rs -> {
            respLoginEntity.set(new RespLoginEntity(0, rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(6)));
            resp.setStatus(HttpServletResponse.SC_OK);
        });


        // 请求失败：-1 成功： 0
        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST)
            return new RespLoginEntity(-1, null, null, null, null);
        return respLoginEntity.get();
    }

    // 注册接口
    @PostMapping("/reg")
    public Integer postReg(@RequestParam("name") String name, @RequestParam("username") String usr, @RequestParam("password") String pwd, HttpServletResponse resp) {

        // 如果数据存在 设置请求失败的标志
        jdbc.query("select * from yyqz.yyqz where UserName = '" + usr + "' limit 1", rs -> {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        });

        // 请求失败：-1 成功： 0
        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST) return -1;

        // 结果成功的同时插入一条用户数据
        jdbc.execute("insert into yyqz.yyqz(Name, UserName, PassWord) values('" + name + "','" + usr + "','" + pwd + "')");
        return 0;
    }

    // 提交喜欢的美食类型
    @PostMapping("/user/liketype") // 苦辣甜咸  0 1 2 3 1,2,3,4,5
    public Integer postLikeType(@RequestParam("username") String usr, @RequestParam("like_type") String likeType, HttpServletResponse resp) {
        if (likeType.length() > 7) {
            return -1;
        }
        for (char c : likeType.toCharArray()) {
            if (c == '0' || c == '1' || c == '2' || c == '3' || c == ',') {
                continue;
            }
            return -1;
        }
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        jdbc.execute("update yyqz.yyqz set likeType = '" + likeType + "' where UserName = '" + usr + "'",
                (CallableStatementCallback<Object>) cs -> {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    return null;
                });
        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST) return -1;
        else return 0;
    }

    // 提交预定的门票
    @PostMapping("/book/tickets")
    public Integer postBookTickets(
            @RequestParam("username") String usr,
            @RequestParam("tickets_count") String ticketsCount,
            @RequestParam("tour_name") String tourName,
            @RequestParam("tour_imagename") String tourImageName,
            @RequestParam("date") String date,
            HttpServletResponse resp
    ) {
        // 获取要预定的票数
        int count = Integer.parseInt(ticketsCount);

        try {
            simpleDateFormat.parse(date);
        } catch (Exception e) {
            e.printStackTrace();
            return -3;
        }

        // 自动引用
        AtomicInteger totalTicketsCount = new AtomicInteger(-1);
        AtomicReference<ArrayList<UserTicketsDatas>> userTicketDatas = new AtomicReference<>(new ArrayList<>());

        // 获取用户预定的门票数据
        jdbc.query("select yyqz.TicketsDatas from yyqz.yyqz where UserName = '" + usr + "' limit 1", rs -> {

            // 如果数据不为空 则设置门票数据
            String ticketsData = rs.getString(1);
            if (ticketsData != null)
                userTicketDatas.set(gson.fromJson(ticketsData, type));
        });

        // 获取总共的门票数量
        jdbc.query("select tour.Tickets from yyqz.tour where Name = '" + tourName + "'", rs1 -> {
            totalTicketsCount.set(Integer.parseInt(rs1.getString(1)) - count);
        });

        // 等于 -2 代表门票不够了
        if (totalTicketsCount.get() == -1) {
            return -2;
        }

        // 如果用户门票数据不为空
        if (userTicketDatas.get() != null) {

            // 获取用户的门票信息
            for (UserTicketsDatas datas : userTicketDatas.get()) {

                // 如果 日期 和 预定门票的日期 相同 景点名称也是 代表门票预定过了
                if (datas.getDate().equals(date) && datas.getTourName().equals(tourName)) {
                    return -1;
                }
            }

        }

        // 添加新的门票
        userTicketDatas.get().add(new UserTicketsDatas(count, tourName, tourImageName, date));

        // 更新数据 --> 门票总数
        jdbc.execute("update yyqz.tour set tour.Tickets = " + totalTicketsCount.get() + " where Name = '" + tourName + "'");

        // 更新数据 --> 用户门票数据
        jdbc.execute("update yyqz.yyqz set yyqz.TicketsDatas = '" + gson.toJson(userTicketDatas.get()) + "' where UserName = '" + usr + "'");
        return 0;


    }

    // 获取预定门票
    @GetMapping("/book/tickets/user")
    public RespBookingTicketsEntity getUserTickets(@RequestParam("username") String usr, HttpServletResponse resp) {

        // 自动引用
        AtomicReference<ArrayList<UserTicketsDatas>> userTicketDatas = new AtomicReference<>(new ArrayList<>());

        // 查询账号是否存在 存在恢复数据或初始化数据
        jdbc.query("select yyqz.TicketsDatas from yyqz.yyqz where UserName = '" + usr + "' limit 1", rs -> {
            // 获取数据 如果数据不为null
            String data = rs.getString(1);
            if (data != null) userTicketDatas.set(gson.fromJson(rs.getString(1), type));
        });

        // 数据为空 代表不存在
        if (userTicketDatas.get().isEmpty()) return new RespBookingTicketsEntity(-1, null);

        return new RespBookingTicketsEntity(0, userTicketDatas.get());
    }

    // 删除预定门票
    @PostMapping("/book/tickets/cancel")
    public int cancelUserTickets(
            @RequestParam("username") String usr,
            @RequestParam("tickets_count") String ticketsCount,
            @RequestParam("tour_name") String tourName, HttpServletResponse resp
    ) {
        // 获取要取消的票数
        int count = Integer.parseInt(ticketsCount);

        // 自动引用
        AtomicReference<ArrayList<UserTicketsDatas>> userTicketDatas = new AtomicReference<>(new ArrayList<>());
        AtomicInteger totalTicketCount = new AtomicInteger(-1);
        userTicketDatas.set(new ArrayList<>());

        // 查询账号是否存在 存在恢复数据或初始化数据
        jdbc.query("select yyqz.TicketsDatas from yyqz.yyqz where UserName = '" + usr + "' limit 1", rs -> {
            // 设置用户门票数据
            userTicketDatas.set(gson.fromJson(rs.getString(1), type));
            // 查询是否存在景点名称
            jdbc.query("select tour.Tickets from yyqz.tour where Name = '" + tourName + "'", rs1 -> {
                totalTicketCount.set(Integer.parseInt(rs1.getString(1)) + count);
            });
        });

        // 景点名称或者账号不存在 则返回 -1
        if (totalTicketCount.get() == -1) return -2;

        // 账号的数据为空 则返回 -2
        if (userTicketDatas.get().isEmpty()) return -2;

        val ticketsDatas = userTicketDatas.get();
        System.out.println(ticketsDatas);
        for (UserTicketsDatas userTicketData : ticketsDatas) {
            if (userTicketData.getTourName().equals(tourName)) {
                if (userTicketData.getBookingTicketsCount() == count) {
                    ticketsDatas.remove(userTicketData);
                    jdbc.execute("update yyqz.tour set tour.Tickets = " + totalTicketCount.get() + " where Name = '" + tourName + "'");
                    jdbc.execute("update yyqz.yyqz set yyqz.TicketsDatas = '" + gson.toJson(ticketsDatas) + "' where UserName = '" + usr + "'");
                    return -1;
                } else if (userTicketData.getBookingTicketsCount() > count && count > 0) {
                    val index = ticketsDatas.indexOf(userTicketData);
                    val remainedCount = ticketsDatas.get(index).getBookingTicketsCount() - count;
                    ticketsDatas.get(index).setBookingTicketsCount(remainedCount);
                    jdbc.execute("update yyqz.tour set tour.Tickets = " + totalTicketCount.get() + " where Name = '" + tourName + "'");
                    jdbc.execute("update yyqz.yyqz set yyqz.TicketsDatas = '" + gson.toJson(ticketsDatas) + "' where UserName = '" + usr + "'");

                    return remainedCount;
                }
            }
        }

        // 当取消预约的票数 和 景点名称对不上时则返回 -1
        return -2;
    }

    // 修改名称
    @PostMapping("/user/change/name")
    public int changeUsrName(@RequestParam("username") String usr, @RequestParam("name") String name, HttpServletResponse resp) {
        // 自动引用
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        // 查询账号是否存在 存在恢复数据或初始化数据
        jdbc.query("select yyqz.TicketsDatas from yyqz.yyqz where UserName = '" + usr + "' limit 1", rs -> {
            resp.setStatus(HttpServletResponse.SC_OK);
        });

        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST) return -1;
        jdbc.execute("update yyqz.yyqz set yyqz.Name = '" + name + "' where UserName = '" + usr + "'");
        return 0;
    }

    // 修改密码
    @PostMapping("/user/change/pwd")
    public int changeUsrPwd(@RequestParam("username") String usr, @RequestParam("new_pwd") String name, HttpServletResponse resp) {

        // 自动引用
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        // 查询账号是否存在 存在恢复数据或初始化数据
        jdbc.query("select yyqz.TicketsDatas from yyqz.yyqz where UserName = '" + usr + "' limit 1", rs -> {
            resp.setStatus(HttpServletResponse.SC_OK);
        });

        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST) return -1;
        jdbc.execute("update yyqz.yyqz set yyqz.PassWord = '" + name + "' where UserName = '" + usr + "'");
        return 0;
    }

    // 修改喜欢的美食类型
    @PostMapping("/user/change/liketype")
    public int changeLikeType(@RequestParam("username") String usr, @RequestParam("like_type") String likeType, HttpServletResponse resp) {
        if (likeType.isEmpty()) return -1;
        else if (likeType.length() > 7) return -1;
        char[] likeTypes = likeType.toCharArray();
        for (char c : likeTypes) {
            if (!(c == '0' || c == '1' || c == '2' || c == '3' || c == ',')) {
                return -1;
            }
        }

        jdbc.execute("update yyqz.yyqz set yyqz.likeType = '" + likeType + "' where UserName = '" + usr + "'");
        return 0;
    }

    // 修改美食评分
    @PostMapping("/user/change/score")
    public int changeFoodScore(@RequestParam("username") String usr, @RequestParam("food_title") String foodTitle, @RequestParam("food_score") String foodScore, HttpServletResponse resp) {
        try {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            AtomicReference<ArrayList<FoodScore>> foodScores = new AtomicReference<>();
            foodScores.set(new ArrayList<>());
            jdbc.query("select yyqz.score from yyqz.yyqz where UserName = '" + usr + "'", rs -> {
                val scores = rs.getString(1);
                if (scores == null || scores.isEmpty()) {
                    foodScores.get().add(new FoodScore(foodTitle, Integer.parseInt(foodScore)));
                    jdbc.execute("update yyqz.yyqz set yyqz.score = '" + gson.toJson(foodScores.get()) + "' where UserName = '" + usr + "'");
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    foodScores.set(gson.fromJson(scores, foodScoreType));
                    ArrayList<FoodScore> foods = (ArrayList<FoodScore>) foodScores.get().clone();
                    for (FoodScore score : foodScores.get()) {
                        if (score.getFoodName().equals(foodTitle)) foods.remove(score);
                    }
                    foods.add(new FoodScore(foodTitle, Integer.parseInt(foodScore)));
                    jdbc.execute("update yyqz.yyqz set yyqz.score = '" + gson.toJson(foods) + "' where UserName = '" + usr + "'");
                    resp.setStatus(HttpServletResponse.SC_OK);
                }
            });
            if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST) return -1;
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // 景点主页
    @GetMapping("/homepage")
    public ArrayList<RespTourEntity> getHomePgae(HttpServletResponse resp) {

        // 先设置 请求失败的标志
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        ArrayList<RespTourEntity> respTourEntityList = new ArrayList<RespTourEntity>();
        jdbc.query("select * from yyqz.tour", rs -> {
            respTourEntityList.add(new RespTourEntity(0,
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7),
                    rs.getString(8),
                    rs.getString(9)));
            while (rs.next()) {
                respTourEntityList.add(new RespTourEntity(0,
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        rs.getString(8),
                        rs.getString(9)));
            }
            resp.setStatus(HttpServletResponse.SC_OK);
        });
        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST) {
            respTourEntityList.add(new RespTourEntity(-1, null, null, null, null, null, null, null, null));
            return respTourEntityList;
        }
        return respTourEntityList;
    }

    // 获取美食主页
    @GetMapping("/food/homepage/liketype")
    public RespFoodEntity foodHomePageLikeType(@RequestParam("username") String usr, HttpServletResponse resp) {

        // 先设置请求失败
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        // 自动引用
        AtomicReference<String> likeType = new AtomicReference<>("");
        AtomicReference<ArrayList<FoodScore>> userSelfScoreDatas = new AtomicReference<>(new ArrayList<>());
        AtomicReference<ArrayList<FoodEntity>> respFoodResult = new AtomicReference<>(new ArrayList<>());

        // 查询用户是否存在
        jdbc.query("select yyqz.yyqz.likeType from yyqz.yyqz where UserName = '" + usr + "' limit 1", rs -> {
            String like = rs.getString(1);
            if (like != null && !like.isEmpty()) {
                likeType.set(like);
            }
            resp.setStatus(HttpServletResponse.SC_OK);
        });

        // 代表查询的用户不存在
        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST) {
            return new RespFoodEntity(-1, null);
        }

        // 继续恢复初始标志位
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        // 查询全部美食
        foodManager.GetAllFood(jdbc, respFoodResult, resp);

        // 代表没有查询到美食
        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST || respFoodResult.get().isEmpty()) {
            return new RespFoodEntity(-1, null);
        }

        // 查询所有用户的用户名和评分列表
        foodManager.GetAllUserFoodScore(jdbc, usr, null, userSelfScoreDatas);


        if (!respFoodResult.get().isEmpty()) {
            return new RespFoodEntity(0, foodManager.GetFoodLikeTypeResultBySort(likeType, respFoodResult, userSelfScoreDatas));
        }
        return new RespFoodEntity(-1, null);
    }

    // 获取美食主页
    @GetMapping("/food/homepage")
    public RespFoodEntity foodHomePageRecommend(@RequestParam("username") String usr, HttpServletResponse resp) {

        // 先设置请求失败
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        // 四个自动引用对象
        AtomicReference<List<Map<String, Integer>>> recommendations = new AtomicReference<>(new ArrayList<>());
        AtomicReference<Map<String, Map<String, Integer>>> otherUserScoreDatas = new AtomicReference<>(new HashMap<>());
        AtomicReference<ArrayList<FoodScore>> userSelfScoreDatas = new AtomicReference<>(new ArrayList<>());
        AtomicReference<ArrayList<FoodEntity>> respFoodResult = new AtomicReference<>(new ArrayList<>());

        // 查询用户是否存在
        jdbc.query("select yyqz.yyqz.UserName from yyqz.yyqz where UserName = '" + usr + "' limit 1", rs -> {
            resp.setStatus(HttpServletResponse.SC_OK);
        });

        // 代表查询的用户不存在
        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST) return new RespFoodEntity(-1, null);

        // 继续恢复初始标志位
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        // 查询所有用户的用户名和评分列表
        foodManager.GetAllUserFoodScore(jdbc, usr, otherUserScoreDatas, userSelfScoreDatas);

        // 如果用户自己的评分 和 其他用户的评分数据 不为空 则执行协同过滤推荐算法
        if (!userSelfScoreDatas.get().isEmpty() && !otherUserScoreDatas.get().isEmpty()) {
            recommendations.set(new CollaborativeFilteringRecommender(otherUserScoreDatas.get()).getRecommendationsForUser(usr));
        }

        // 查询全部美食
        foodManager.GetAllFood(jdbc, respFoodResult, resp);

        // 代表没有查询到美食
        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST || respFoodResult.get().isEmpty()) {
            return new RespFoodEntity(-1, null);
        }

        return new RespFoodEntity(0, foodManager.GetFoodRecommendResultBySort(recommendations, respFoodResult, userSelfScoreDatas));
    }

    @GetMapping("/download")
    public void download(@RequestParam("file_name") String fileName, HttpServletResponse response) {
        try {
            // path是指想要下载的文件的路径
            File file = new File("C://app/yyqz/mp3", fileName);
            // 获取文件名
            String filename = file.getName();
            // 获取文件后缀名
            String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

            // 将文件写入输入流
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStream fis = new BufferedInputStream(fileInputStream);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();

            // 清空response
            response.reset();
            // 设置response的Header
            response.setCharacterEncoding("UTF-8");
            //Content-Disposition的作用：告知浏览器以何种方式显示响应返回的文件，用浏览器打开还是以附件的形式下载到本地保存
            //attachment表示以附件方式下载 inline表示在线打开 "Content-Disposition: inline; filename=文件名.mp3"
            // filename表示文件的默认名称，因为网络传输只支持URL编码的相关支付，因此需要将文件名URL编码后进行传输,前端收到后需要反编码才能获取到真正的名称
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
            // 告知浏览器文件的大小
            response.addHeader("Content-Length", "" + file.length());
            OutputStream outputStream = new BufferedOutputStream(response.getOutputStream());
            response.setContentType("application/octet-stream");
            outputStream.write(buffer);
            outputStream.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @GetMapping("/tour/isstart")
    public int GetTourIsStart(@RequestParam("username") String usr, @RequestParam("tour_name") String tourName, HttpServletResponse resp) {

        // 先设置请求失败
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        // 如果查询到了数据 则请求成功
        AtomicReference<ArrayList<RespStartTourEntity>> startTourEntities = new AtomicReference<>();
        jdbc.query("select * from yyqz.yyqz_start where UserName = '" + usr + "' limit 1", rs -> {
            val data = rs.getString(3);
            if (data != null) {
                startTourEntities.set(gson.fromJson(data, new TypeToken<ArrayList<RespStartTourEntity>>() {
                }.getType()));
                resp.setStatus(HttpServletResponse.SC_OK);
            }
        });

        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST) {
            return -1;
        }

        AtomicInteger code = new AtomicInteger(-1);
        startTourEntities.get().forEach(respStartTourEntity -> {
            if (Objects.equals(respStartTourEntity.getName(), tourName)) {
                if (respStartTourEntity.getIsStart()) {
                    code.set(1);
                } else {
                    code.set(0);
                }
            }
        });
        return code.get();
    }

    @PostMapping("/tour/start")
    public int ChangeTourStart(@RequestParam("username") String usr, @RequestParam("tour_name") String tourName, @RequestParam("tour_start") String isStart, HttpServletResponse resp) {

        // 先设置请求失败
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        // 如果查询到了数据 则请求成功
        AtomicReference<ArrayList<RespStartTourEntity>> startTourEntities = new AtomicReference<>(new ArrayList<>());
        jdbc.query("select * from yyqz.yyqz where UserName = '"+ usr +"' limit 1", rs -> {
            resp.setStatus(HttpServletResponse.SC_OK);
        });

        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST) {
            return -1;
        }

        AtomicInteger isUserExists = new AtomicInteger(-1);
        jdbc.query("select * from yyqz.yyqz_start where UserName = '" + usr + "' limit 1", rs -> {
            val data = rs.getString(3);
            if (data != null) {
                startTourEntities.set(gson.fromJson(data, new TypeToken<ArrayList<RespStartTourEntity>>() {}.getType()));
            }
            isUserExists.set(0);
        });

        if (isUserExists.get() == -1) {
            jdbc.execute("insert into yyqz.yyqz_start(UserName) values('" + usr + "')");
        }

        AtomicInteger code = new AtomicInteger(-1);
        startTourEntities.get().forEach(respStartTourEntity -> {
           if (Objects.equals(respStartTourEntity.getName(), tourName)) {
               respStartTourEntity.setIsStart(Boolean.valueOf(isStart));
               code.set(0);
           }
        });

        if (code.get() == -1) {
            startTourEntities.get().add(new RespStartTourEntity(tourName, Boolean.valueOf(isStart)));
        }


        jdbc.execute("update yyqz.yyqz_start set yyqz_start.StartTour = '" + gson.toJson(startTourEntities.get()) + "' where UserName = '" + usr + "'");
        return 0;
    }

    @GetMapping("/tour/start/exists")
    public ArrayList<RespStartTourEntity> GetTourStart(@RequestParam("username") String usr, HttpServletResponse resp) {

        // 先设置请求失败
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        // 如果查询到了数据 则请求成功
        AtomicReference<ArrayList<RespStartTourEntity>> startTourEntities = new AtomicReference<>();
        jdbc.query("select * from yyqz.yyqz_start where UserName = '" + usr + "' limit 1", rs -> {
            val data = rs.getString(3);
            if (data != null) {
                startTourEntities.set(gson.fromJson(data, new TypeToken<ArrayList<RespStartTourEntity>>() {
                }.getType()));
                resp.setStatus(HttpServletResponse.SC_OK);
            }
        });

        if (resp.getStatus() == HttpServletResponse.SC_BAD_REQUEST) {
            return new ArrayList<>();
        } else {
            return startTourEntities.get();
        }
    }
}