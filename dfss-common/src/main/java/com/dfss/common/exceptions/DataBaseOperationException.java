// 文件：com/dfsss/data/exception/DataBaseOperationException.java
package com.dfss.common.exceptions;

/**
 * <p>统一的数据操作运行时异常。所有 DataBaseOperation 中的异常场景都使用此类抛出，</p>
 * <p>并通过 {@link DataBaseErrorCode} 指定具体的错误类型与默认提示语。</p>
 *
 * <h3>示例：</h3>
 * <pre>
 * throw new DataBaseOperationException(DataBaseErrorCode.INVALID_ENTITY);
 * throw new DataBaseOperationException(DataBaseErrorCode.MAPPER_NOT_FOUND, "未找到 UserMapper，请确认是否已配置。");
 * </pre>
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>{@code errorCode}：对应 {@link DataBaseErrorCode#getCode()}。</li>
 *   <li>{@code errorMessage}：优先使用构造函数传入的自定义消息，若为 null 则使用 {@link DataBaseErrorCode#getMessage()}。</li>
 * </ul>
 *
 * @author shushun
 * @since 2025-06-02
 */
public class DataBaseOperationException extends RuntimeException {

    /** 错误码，例如 "DB_OP_001" */
    private final String errorCode;

    /** 错误消息，优先使用自定义；否则使用枚举默认提示 */
    private final String errorMessage;

    /**
     * 使用枚举指定错误类型，并使用其默认消息。
     *
     * @param errorCodeEnum 枚举项
     */
    public DataBaseOperationException(DataBaseErrorCode errorCodeEnum) {
        super(errorCodeEnum.getMessage());
        this.errorCode = errorCodeEnum.getCode();
        this.errorMessage = errorCodeEnum.getMessage();
    }

    /**
     * 使用枚举指定错误类型，并提供更详细的自定义消息。
     *
     * @param errorCodeEnum 枚举项
     * @param customMessage 自定义错误提示，会覆盖枚举默认提示
     */
    public DataBaseOperationException(DataBaseErrorCode errorCodeEnum, String customMessage) {
        super(customMessage);
        this.errorCode = errorCodeEnum.getCode();
        this.errorMessage = customMessage;
    }

    /**
     * 使用枚举指定错误类型，并在构造时携带底层异常。
     * 错误消息使用枚举默认提示。
     *
     * @param errorCodeEnum 枚举项
     * @param cause         底层异常
     */
    public DataBaseOperationException(DataBaseErrorCode errorCodeEnum, Throwable cause) {
        super(errorCodeEnum.getMessage(), cause);
        this.errorCode = errorCodeEnum.getCode();
        this.errorMessage = errorCodeEnum.getMessage();
    }

    /**
     * 使用枚举指定错误类型，并提供自定义消息与底层异常。
     *
     * @param errorCodeEnum 枚举项
     * @param customMessage 自定义错误提示，会覆盖枚举默认提示
     * @param cause         底层异常
     */
    public DataBaseOperationException(DataBaseErrorCode errorCodeEnum, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCodeEnum.getCode();
        this.errorMessage = customMessage;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码字符串
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误消息。
     *
     * @return 自定义或枚举默认错误消息
     */
    @Override
    public String getMessage() {
        return errorMessage;
    }
}
