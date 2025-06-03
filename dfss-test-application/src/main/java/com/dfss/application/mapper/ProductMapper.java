package com.dfss.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dfss.application.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus 的 Mapper 接口，用来执行 Product 实体相关的 CRUD 操作
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    // 如果需要自定义 SQL 方法，可以在这里新增方法签名，然后在相应的 XML 或注解里编写 SQL。
}
