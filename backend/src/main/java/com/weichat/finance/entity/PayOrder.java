package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务订单表实体（商户侧订单）。
 *
 * <p>对应表 t_pay_order，金额单位：分。</p>
 *
 * <p>显式编写 getter/setter 避免 lombok 在 maven-compiler-plugin 的 BUG。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@TableName("t_pay_order")
public class PayOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String outTradeNo;
    private String mchId;
    private String appId;
    private String description;
    private Long amountTotal;
    private String currency;
    private String openid;
    private String productType;
    private String status;
    private LocalDateTime timeExpire;
    private LocalDateTime successTime;
    private String notifyUrl;
    private String attach;
    private String ext;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @TableLogic
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOutTradeNo() { return outTradeNo; }
    public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
    public String getMchId() { return mchId; }
    public void setMchId(String mchId) { this.mchId = mchId; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAmountTotal() { return amountTotal; }
    public void setAmountTotal(Long amountTotal) { this.amountTotal = amountTotal; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getTimeExpire() { return timeExpire; }
    public void setTimeExpire(LocalDateTime timeExpire) { this.timeExpire = timeExpire; }
    public LocalDateTime getSuccessTime() { return successTime; }
    public void setSuccessTime(LocalDateTime successTime) { this.successTime = successTime; }
    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
    public String getAttach() { return attach; }
    public void setAttach(String attach) { this.attach = attach; }
    public String getExt() { return ext; }
    public void setExt(String ext) { this.ext = ext; }
    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }
    public LocalDateTime getGmtModified() { return gmtModified; }
    public void setGmtModified(LocalDateTime gmtModified) { this.gmtModified = gmtModified; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
