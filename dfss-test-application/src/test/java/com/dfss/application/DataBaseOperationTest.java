// 文件：src/test/java/com/dfss/application/DataBaseOperationTest.java
package com.dfss.application;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dfss.data.util.DataBaseOperation;
import com.dfss.application.entity.Product;
import com.dfss.application.entity.User;
import com.dfss.application.vo.UserVO;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * DataBaseOperation 在 Spring Boot 3.5.x 环境下的集成测试
 */
@SpringBootTest
//@Transactional    // 每个测试方法回滚，保持数据库干净；若不需要可删除
class DataBaseOperationTest {


    @Test
    void save_list_getOne_update_delete_works() {
        // 2. 插入几条
        User u1 = new User();
        u1.setName("Tom");
        u1.setEmail("tom@example.com");
        DataBaseOperation.insert(u1);


        // 2. 插入几条
        Product p1 = new Product();
        p1.setName("Tom");
        p1.setDescription("tom@example.com");
        DataBaseOperation.insert(p1);


        User query1 = new User();
        u1.setName("Tom");

        IPage<UserVO> voPage = DataBaseOperation.page(query1, UserVO.class, 1, 2);

        List<UserVO> vos = voPage.getRecords();
        long totalCount = voPage.getTotal();
        long totalPages = voPage.getPages();
        System.out.println(JSONArray.toJSONString(vos));
    }
}
