package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时查单执行日志表实体。
 *
 * <p>对应表 t_pay_order_query_log。记录每次调度批次的统计信息（笔数/耗时/异常），便于排障和监控。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_pay_order_query_log")
@Schema(description = "定时查单执行日志表（t_pay_order_query_log）")
public class PayOrderQueryLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID（自增）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "本批次唯一标识（时间戳+随机）", example = "20260914143000_a1b2c3d4")
    private String batchNo;

    @Schema(description = "商户号（空=全部商户）", example = "1900000109")
    private String mchId;

    @Schema(description = "本批次扫描开始时间", example = "2026-09-14T14:30:00")
    private LocalDateTime scanStartTime;

    @Schema(description = "本批次扫描结束时间", example = "2026-09-14T14:30:05")
    private LocalDateTime scanEndTime;

    @Schema(description = "本次扫描出的悬挂单数量", example = "3")
    private Integer scanOrderCount;

    @Schema(description = "本次实际调用查单接口次数", example = "3")
    private Integer queryOrderCount;

    @Schema(description = "查单成功次数", example = "3")
    private Integer successCount;

    @Schema(description = "查单失败次数", example = "0")
    private Integer failCount;

    @Schema(description = "仍为 NOTPAY（未变化）", example = "2")
    private Integer notpayCount;

    @Schema(description = "状态变为 CLOSED", example = "1")
    private Integer closedCount;

    @Schema(description = "状态发生变化（兜底生效）", example = "1")
    private Integer updatedCount;

    @Schema(description = "本批次总耗时（毫秒）", example = "3500")
    private Long costMs;

    @Schema(description = "本批次异常摘要（最严重的一条）", example = "微信接口返回 500")
    private String errorMessage;

    @Schema(description = "创建时间（MyBatis-Plus 自动填充）", example = "2026-09-14T14:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;
}
