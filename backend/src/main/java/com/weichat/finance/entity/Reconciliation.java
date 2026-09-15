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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账记录汇总表实体。
 *
 * <p>对应表 t_pay_reconciliation。每天对账后生成一条汇总记录，
 * 含总笔数/总金额/差异笔数/差异金额。</p>
 *
 * <p>v1.1 启用。流程：</p>
 * <ol>
 *   <li>每日凌晨下载昨天账单文件</li>
 *   <li>解析账单与本地 DB 比对</li>
 *   <li>写入汇总 + 差异明细</li>
 * </ol>
 *
 * @author panhw
 * @since 2026-09-15
 */
@Data
@TableName("t_pay_reconciliation")
@Schema(description = "对账记录汇总表（t_pay_reconciliation）")
public class Reconciliation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID（自增）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商户号", example = "1900000109")
    private String mchId;

    @Schema(description = "账单日期", example = "2026-09-14")
    private LocalDate billDate;

    @Schema(description = "账单类型", example = "ALL",
        allowableValues = {"ALL", "SUCCESS", "REFUND"})
    private String billType;

    @Schema(description = "微信账单总笔数", example = "100")
    private Integer totalCount;

    @Schema(description = "微信账单总金额（分）", example = "10000")
    private Long totalAmount;

    @Schema(description = "差异笔数", example = "2")
    private Integer diffCount;

    @Schema(description = "差异金额（分，正数=本地少收，负数=本地多收）", example = "100")
    private Long diffAmount;

    @Schema(description = "处理状态",
        example = "SUCCESS",
        allowableValues = {"PENDING", "PROCESSING", "SUCCESS", "FAILED"})
    private String status;

    @Schema(description = "本地账单文件路径（下载到本地后的 gzip 文件路径）",
        example = "/var/data/weichat-finance/bill/2026-09-14.gz")
    private String localFilePath;

    @Schema(description = "创建时间（MyBatis-Plus 自动填充）", example = "2026-09-15T03:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @Schema(description = "更新时间（MyBatis-Plus 自动填充）", example = "2026-09-15T03:05:00", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;
}
