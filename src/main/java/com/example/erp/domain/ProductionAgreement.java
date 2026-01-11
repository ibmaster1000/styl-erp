package com.example.erp.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "production_agreements")
public class ProductionAgreement extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prd_agree_id")
    private Long prdAgreeId;

    @Column(name = "agreement_code", nullable = false, length = 50)
    private String agreementCode;

    @Column(name = "style_code", length = 50)
    private String styleCode;
    
    @Column(name = "styles_id")
    private Long stylesId;

    @Column(name = "production_manager", length = 50)
    private String productionManager;

    @Column(name = "color_type", length = 30)
    private String colorType;

    @Column(name = "color_code", length = 30)
    private String colorCode;

    @Column(name = "size_type", length = 30)
    private String sizeType;

    @Column(name = "size_code", length = 30)
    private String sizeCode;

    private Integer quantity;

    @Column(length = 30)
    private String status;

    public String getAgreementCode() {
        return agreementCode;
    }
    
    public Long getPrdAgreeId() {
        return prdAgreeId;
    }

    public void setPrdAgreeId(Long prdAgreeId) {
        this.prdAgreeId = prdAgreeId;
    }

    public void setAgreementCode(String agreementCode) {
        this.agreementCode = agreementCode;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public Long getStylesId() {
        return stylesId;
    }

    public void setStylesId(Long stylesId) {
        this.stylesId = stylesId;
    }
    
    public String getProductionManager() {
        return productionManager;
    }

    public void setProductionManager(String productionManager) {
        this.productionManager = productionManager;
    }

    public String getColorType() {
        return colorType;
    }

    public void setColorType(String colorType) {
        this.colorType = colorType;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getSizeType() {
        return sizeType;
    }

    public void setSizeType(String sizeType) {
        this.sizeType = sizeType;
    }

    public String getSizeCode() {
        return sizeCode;
    }

    public void setSizeCode(String sizeCode) {
        this.sizeCode = sizeCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
