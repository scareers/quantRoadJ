package com.scareers.gui.ths.simulation.order;

import java.util.Map;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/23/023-19:37:36
 */
public class SellOrder extends Order {
    private static long DEFAULT_PRIORITY = Order.PRIORITY_HIGH;

    public SellOrder(Map<String, Object> params) {
        super("sell", params);
        this.setPriority(DEFAULT_PRIORITY);
    }
}
