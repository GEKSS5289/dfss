// User.java
package com.dfss.application.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 示例实体类 User，对应 H2 中的 user 表
 */
@TableName("user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String email;

    public User() {}

    public Long getId() {
        return id;
    }

    public User setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public User setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public User setEmail(String email) {
        this.email = email;
        return this;
    }
}
