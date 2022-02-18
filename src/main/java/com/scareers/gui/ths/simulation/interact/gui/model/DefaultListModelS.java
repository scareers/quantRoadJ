package com.scareers.gui.ths.simulation.interact.gui.model;

import com.alee.laf.list.ListDataAdapter;
import oshi.hardware.platform.mac.MacDisplay;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * description: 列表使用的可变model, 新增 刷新功能(检测新的项目并添加), 新增 排序功能,
 * 泛型T 应当实现比较接口,  OrderPo 依据时间戳进行比较
 *
 * @author: admin
 * @date: 2022/1/18/018-09:44:56
 */
public class DefaultListModelS<T extends Comparable> extends DefaultListModel<T> {
    public DefaultListModelS() {
        super();
        this.addListDataListener(new ListDataAdapter());
    }

    /**
     * model更新全部传递来的数据. 数量最大数量由调用方控制,非model控制. 且顺序也由调用方控制
     *
     * @param newList
     */
    public void flush(List<T> newList0) {
        ArrayList<T> newList = new ArrayList<>(newList0);
        Collections.sort(newList); // 不对原列表进行更新, 保证稳定性
        // 当元素发生变化, 才更新
        for (int i = 0; i < Math.min(newList.size(), this.getSize()); i++) {
            try {
                if (!this.get(i).equals(newList.get(i))) {
                    this.set(i, newList.get(i));
                }
            } catch (Exception ignored) {
            }
        }
        if (newList.size() > this.getSize()) {
            this.addAll(newList.subList(this.getSize(), newList.size()));
        } else if (newList.size() < this.getSize()) {
            this.removeRange(newList.size(), this.getSize() - 1); // 注意大的index需要-1
        }
    }
}
