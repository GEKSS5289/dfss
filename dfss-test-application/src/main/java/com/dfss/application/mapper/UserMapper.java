// UserMapper.java
package com.dfss.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;


import com.dfss.application.entity.User;
import org.apache.ibatis.annotations.Mapper;


/**
 * UserMapper 继承自 BaseMapper<User>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 直接继承 BaseMapper 即可
}
