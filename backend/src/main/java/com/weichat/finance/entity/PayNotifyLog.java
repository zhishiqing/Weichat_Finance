package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.weichat.finance.entity.enums.CommonFlag;
import com.weichat.finance.entity.enums.NotifyResult;
import com.weichat.finance.entity.enums.NotifyType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微信支付回调原始报文表实体。
 *
 * <p>对应表 t_pay_notify_log。微信支付回调先落库，再处理；用于排障 + 重放。</p>
 *
 * <p>每条记录保存：原始 headers（用于验签）、原始 body（用于解签 / 排障）、处理结果、错误信息。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_pay_notify_log")
public class PayNotifyLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID（自增） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 回调类型。
     *
     * @see NotifyType#PAY 支付成功通知
     * @see NotifyType#REFUND 退款结果通知
     */
    private String notifyType;

    /** 商户订单号（PAY 回调） */
    private String outTradeNo;

    /** 商户退款单号（REFUND 回调） */
    private String outRefundNo;

    /** 商户号 */
    private String mchId;

    /** 请求头（含 Wechatpay-Signature / Timestamp / Nonce / Serial，JSON 格式存储） */
    private String headers;

    /** 请求体原文（验签前，用于排障 + 重新验签） */
    private String rawBody;

    /** 解密后回调内容（resource.ciphertext 解密后的 JSON） */
    private String decryptedBody;

    /**
     * 签名校验结果。
     *
     * @see NotifyResult#PASS 通过
     * @see NotifyResult#FAIL 失败
     * @see NotifyResult#SKIPPED_PHASE3 跳过（Phase 2 占位）
     */
    private String verifyResult;

    /**
     * 业务处理结果。
     *
     * @see NotifyResult#SUCCESS 成功
     * @see NotifyResult#FAILED 失败
     * @see NotifyResult#IGNORED 忽略
     * @see NotifyResult#IGNORED_PHASE3 Phase 2 占位忽略
     */
    private String processResult;

    /** 错误信息（验签失败 / 业务处理异常等，最长 1024 字符） */
    private String errorMessage;

    /** 创建时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 逻辑删除标记。
     *
     * @see CommonFlag#NOT_DELETED 未删
     * @see CommonFlag#DELETED 已删
     */
    @TableLogic
    private Integer isDeleted;
}
