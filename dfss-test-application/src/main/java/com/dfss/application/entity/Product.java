package com.dfss.application.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


import java.io.Serializable;

/**
 * 商品表对应的实体类
 */
@Data
@TableName("product")  // 假设数据库中表名是 tb_product，可根据实际情况修改
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId
    private Long id;

    /** 商品名称 */
    private String name;

    /** 简短描述 */
    private String description;

}
