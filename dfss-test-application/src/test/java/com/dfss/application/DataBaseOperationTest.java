// 文件：src/test/java/com/dfss/application/DataBaseOperationTest.java
package com.dfss.application;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dfss.data.util.DataBaseOperation;
import com.dfss.application.entity.Product;
import com.dfss.application.entity.User;
import com.dfss.application.vo.UserVO;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * DataBaseOperation 在 Spring Boot 3.5.x 环境下的集成测试
 */
@SpringBootTest
@Slf4j
class DataBaseOperationTest {


    @Test
    void save_list_getOne_update_delete_works() {
//        // 2. 插入几条
//        User u1 = new User();
//        u1.setName("Tom");
//        u1.setEmail("tom@example.com");
//        DataBaseOperation.insert(u1);
//
//
//        // 2. 插入几条
//        Product p1 = new Product();
//        p1.setName("Tom");
//        p1.setDescription("tom@example.com");
//        DataBaseOperation.insert(p1);
//
//
//        User query1 = new User();
//        query1.setName("Tom");
//
//        IPage<UserVO> voPage = DataBaseOperation.page(query1, UserVO.class, 1, 10);
//        log.info("{}", JSON.toJSONString(voPage));
//        List<UserVO> vos = voPage.getRecords();
//        log.info("{}", JSON.toJSONString(vos));


        List<User> users = DataBaseOperation.listByWrapper(
                User.class,
                DataBaseOperation.lambdaQuery(User.class)
                        .isNotNull(User::getEmail)
        );

        log.info("{}", JSON.toJSONString(users));
    }
}
