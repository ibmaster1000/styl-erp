package com.example.erp.controller.dto;

public class StyleSearchCondition {

    private final String styleCodeLike;
    private final String itemLike;
    private final String designerNameLike;
    private final Integer isActive;
    private final String sort;
    private final String direction;

    public StyleSearchCondition(String styleCodeLike,
                                String itemLike,
                                String designerNameLike,
                                Integer isActive,
                                String sort,
                                String direction) {
        this.styleCodeLike = styleCodeLike;
        this.itemLike = itemLike;
        this.designerNameLike = designerNameLike;
        this.isActive = isActive;
        this.sort = sort;
        this.direction = direction;
    }

    public String getStyleCodeLike() {
        return styleCodeLike;
    }

    public String getItemLike() {
        return itemLike;
    }

    public String getDesignerNameLike() {
        return designerNameLike;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public String getSort() {
        return sort;
    }

    public String getDirection() {
        return direction;
    }
}
