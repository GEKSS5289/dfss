// 文件：src/test/java/com/dfss/application/DataBaseOperationTest.java
package com.dfss.application;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dfss.data.util.DataBaseOperation;
import com.dfss.application.entity.Product;
import com.dfss.application.entity.User;
import com.dfss.application.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;

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
        // insert user
        User user = new User();
        user.setName("Tom");
        user.setEmail("tom@example.com");
        int userInsert = DataBaseOperation.insert(user);
        Assertions.assertThat(userInsert).isEqualTo(1);
        Assertions.assertThat(user.getId()).isNotNull();

        // insert product
        Product product = new Product();
        product.setName("Book");
        product.setDescription("A book");
        int productInsert = DataBaseOperation.insert(product);
        Assertions.assertThat(productInsert).isEqualTo(1);
        Assertions.assertThat(product.getId()).isNotNull();

        // query page
        User query = new User();
        query.setName("Tom");
        IPage<UserVO> page = DataBaseOperation.page(query, UserVO.class, 1, 10);
        Assertions.assertThat(page.getTotal()).isEqualTo(1);
        Assertions.assertThat(page.getRecords()).hasSize(1);
        Assertions.assertThat(page.getRecords().get(0).getEmail()).isEqualTo("tom@example.com");

        // update
        user.setEmail("new@example.com");
        DataBaseOperation.updateById(user);
        User afterUpdate = DataBaseOperation.getById(User.class, user.getId());
        Assertions.assertThat(afterUpdate.getEmail()).isEqualTo("new@example.com");

        // delete
        DataBaseOperation.deleteById(User.class, user.getId());
        DataBaseOperation.deleteById(Product.class, product.getId());
        Assertions.assertThat(DataBaseOperation.getById(User.class, user.getId())).isNull();
        Assertions.assertThat(DataBaseOperation.getById(Product.class, product.getId())).isNull();
    }
}
