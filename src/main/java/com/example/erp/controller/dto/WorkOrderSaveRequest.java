package com.example.erp.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public class WorkOrderSaveRequest {
    private String styleCode;
    private String prdAgreeCode;
    private String colorCode;
    private BigDecimal agreementQuantity;
    private Long prdAgreeId;
    private String producerCode;
    private String dueDate;
    private String deliveryPlaceCode;
    private List<WorkOrderSaveLine> lines;

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public String getPrdAgreeCode() {
        return prdAgreeCode;
    }

    public void setPrdAgreeCode(String prdAgreeCode) {
        this.prdAgreeCode = prdAgreeCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public BigDecimal getAgreementQuantity() {
        return agreementQuantity;
    }

    public void setAgreementQuantity(BigDecimal agreementQuantity) {
        this.agreementQuantity = agreementQuantity;
    }

    public Long getPrdAgreeId() {
        return prdAgreeId;
    }

    public void setPrdAgreeId(Long prdAgreeId) {
        this.prdAgreeId = prdAgreeId;
    }

    public String getProducerCode() {
        return producerCode;
    }

    public void setProducerCode(String producerCode) {
        this.producerCode = producerCode;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getDeliveryPlaceCode() {
        return deliveryPlaceCode;
    }

    public void setDeliveryPlaceCode(String deliveryPlaceCode) {
        this.deliveryPlaceCode = deliveryPlaceCode;
    }

    public List<WorkOrderSaveLine> getLines() {
        return lines;
    }

    public void setLines(List<WorkOrderSaveLine> lines) {
        this.lines = lines;
    }

    public static class WorkOrderSaveLine {
        private String styleCode;
        private String prdAgreeCode;
        private String colorCode;
        private BigDecimal agreementQuantity;
        private Long prdAgreeId;
        private String producerCode;
        private String dueDate;
        private String deliveryPlaceCode;

        public String getStyleCode() {
            return styleCode;
        }

        public void setStyleCode(String styleCode) {
            this.styleCode = styleCode;
        }

        public String getPrdAgreeCode() {
            return prdAgreeCode;
        }

        public void setPrdAgreeCode(String prdAgreeCode) {
            this.prdAgreeCode = prdAgreeCode;
        }

        public String getColorCode() {
            return colorCode;
        }

        public void setColorCode(String colorCode) {
            this.colorCode = colorCode;
        }

        public BigDecimal getAgreementQuantity() {
            return agreementQuantity;
        }

        public void setAgreementQuantity(BigDecimal agreementQuantity) {
            this.agreementQuantity = agreementQuantity;
        }

        public Long getPrdAgreeId() {
            return prdAgreeId;
        }

        public void setPrdAgreeId(Long prdAgreeId) {
            this.prdAgreeId = prdAgreeId;
        }

        public String getProducerCode() {
            return producerCode;
        }

        public void setProducerCode(String producerCode) {
            this.producerCode = producerCode;
        }

        public String getDueDate() {
            return dueDate;
        }

        public void setDueDate(String dueDate) {
            this.dueDate = dueDate;
        }

        public String getDeliveryPlaceCode() {
            return deliveryPlaceCode;
        }

        public void setDeliveryPlaceCode(String deliveryPlaceCode) {
            this.deliveryPlaceCode = deliveryPlaceCode;
        }
    }
}
