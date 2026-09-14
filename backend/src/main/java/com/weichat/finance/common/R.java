package com.weichat.finance.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应封装
 *
 * @param <T> 数据类型
 * @author panhw
 * @since 2026-09-14
 */
@Data
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态码：0 成功，其他失败 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 数据 */
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(0);
        r.setMessage("成功");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }

    public static <T> R<T> fail(Integer code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
