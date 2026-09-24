package com.example.rule.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则事实对象（fact）：规则只负责"改状态/累加数值"，最终金额与文案由服务层算，
 * 这样规则保持纯粹，也不踩 mvel 方言下 update() 退化成全量更新的坑（详见 ANALYSIS.md 5.1）。
 */
public class Order {

    /** 客户等级：VIP / GOLD / NORMAL */
    private String customerLevel = "NORMAL";
    /** 收货区域：新疆 / 西藏 / 内蒙古 / 北京 ... */
    private String region = "北京";
    /** 订单金额（元） */
    private double amount;
    /** 商品件数 */
    private int itemCount;
    /** 规则累加：折扣率 0~1 */
    private double discount;
    /** 规则累加：运费 */
    private double shippingFee;
    /** 规则置位：风控/库存拦截 */
    private boolean rejected;
    /** 规则置位：大额订单标记 */
    private boolean bigOrder;
    /** 命中说明（服务层按规则结果生成，不放规则 RHS 里） */
    private final List<String> messages = new ArrayList<>();

    public Order() {
    }

    public Order(String customerLevel, String region, double amount, int itemCount) {
        this.customerLevel = customerLevel;
        this.region = region;
        this.amount = amount;
        this.itemCount = itemCount;
    }

    public String getCustomerLevel() {
        return customerLevel;
    }

    public void setCustomerLevel(String customerLevel) {
        this.customerLevel = customerLevel;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(double shippingFee) {
        this.shippingFee = shippingFee;
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public boolean isBigOrder() {
        return bigOrder;
    }

    public void setBigOrder(boolean bigOrder) {
        this.bigOrder = bigOrder;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void addMessage(String message) {
        this.messages.add(message);
    }

    /** 被拦截 => 总额 0；折扣作用在商品金额上，运费单收 */
    public double getFinalAmount() {
        if (rejected) {
            return 0.0;
        }
        return round2(amount * (1 - discount) + shippingFee);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return "Order{customerLevel=" + customerLevel + ", region=" + region + ", amount=" + amount
                + ", itemCount=" + itemCount + ", discount=" + discount + ", shippingFee=" + shippingFee
                + ", rejected=" + rejected + ", bigOrder=" + bigOrder + ", finalAmount=" + getFinalAmount() + "}";
    }
}
