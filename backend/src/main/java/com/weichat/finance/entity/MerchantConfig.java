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
 * 商户配置表实体。
 *
 * <p>对应表 t_merchant_config。</p>
 *
 * <p>显式编写 getter/setter 避免 lombok 在 maven-compiler-plugin 的 BUG。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@TableName("t_merchant_config")
public class MerchantConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String mchId;
    private String appId;
    private String merchantName;
    private String mode;
    private String apiV3Key;
    private String certSerialNo;
    private String certPrivateKeyPath;
    private String notifyUrlBase;
    private String v2Key;
    private Integer enabled;
    private String ext;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @TableLogic
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMchId() { return mchId; }
    public void setMchId(String mchId) { this.mchId = mchId; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getApiV3Key() { return apiV3Key; }
    public void setApiV3Key(String apiV3Key) { this.apiV3Key = apiV3Key; }
    public String getCertSerialNo() { return certSerialNo; }
    public void setCertSerialNo(String certSerialNo) { this.certSerialNo = certSerialNo; }
    public String getCertPrivateKeyPath() { return certPrivateKeyPath; }
    public void setCertPrivateKeyPath(String certPrivateKeyPath) { this.certPrivateKeyPath = certPrivateKeyPath; }
    public String getNotifyUrlBase() { return notifyUrlBase; }
    public void setNotifyUrlBase(String notifyUrlBase) { this.notifyUrlBase = notifyUrlBase; }
    public String getV2Key() { return v2Key; }
    public void setV2Key(String v2Key) { this.v2Key = v2Key; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public String getExt() { return ext; }
    public void setExt(String ext) { this.ext = ext; }
    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }
    public LocalDateTime getGmtModified() { return gmtModified; }
    public void setGmtModified(LocalDateTime gmtModified) { this.gmtModified = gmtModified; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
