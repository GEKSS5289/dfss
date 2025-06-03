// 文件：com/dfsss/data/exception/DataBaseErrorCode.java
package com.dfss.exception;

/**
 * <p>数据库操作异常枚举。列举了 DataBaseOperation 中可能抛出的各种错误类型。</p>
 * <p>每个枚举项都包含一个唯一的错误码（code）和对应的默认错误消息（message）。</p>
 *
 * <pre>
 * 常见用法：
 * throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
 * throw new DataBaseOperationException(DataBaseErrorCode.MAPPER_NOT_FOUND, "自定义更详细的提示信息");
 * </pre>
 *
 * @author shushun
 * @since 2025-06-02
 */
public enum DataBaseErrorCode {
    /**
     * 传入的实体或 DTO 对象为 null。
     */
    INVALID_ENTITY("DB_OP_001", "实体或 DTO 对象不能为空。"),

    /**
     * 没有在 mapperMap 中找到对应实体的 BaseMapper。
     */
    MAPPER_NOT_FOUND("DB_OP_002", "未找到对应的 Mapper，请检查 Mapper 是否已注册并继承 BaseMapper。"),

    /**
     * 检测到多个 Mapper 对应同一实体，导致映射冲突。
     */
    DUPLICATE_MAPPER("DB_OP_003", "检测到多个 Mapper 对应同一实体，请保证映射唯一。"),

    /**
     * 无法实例化目标实体类，通常是因为缺少无参构造函数或访问权限不足。
     */
    ENTITY_INSTANTIATION_FAILED("DB_OP_004", "无法实例化目标实体，请确保实体具有公共无参构造函数。");

    /** 错误码 */
    private final String code;
    /** 默认错误消息 */
    private final String message;

    DataBaseErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码（code）。
     *
     * @return 示例 "DB_OP_001"
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取默认错误消息。
     *
     * @return 示例 "实体或 DTO 对象不能为空。"
     */
    public String getMessage() {
        return message;
    }
}
