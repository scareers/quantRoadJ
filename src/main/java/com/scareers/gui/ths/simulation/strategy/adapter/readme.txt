低买高卖策略执行逻辑:

状态类: // 高卖与低买两类, 关心的参数不同
    LbState
    HsState
        该类表示影响低买高卖决策的 所有相关参数. 每种因子, 都将可能对状态中某项参数进行修改!
        核心方法:
            决策卖出仓位方法
            决策买入仓位方法
因子基类:
    LbFactor
    HsFactor
        表示一个具体因子,  将对 LbState/HsState,  中某些属性进行设置, 将对某些属性进行修改, 以表达"因子影响了仓位决策"语义
        @key: 因子本身可设置某些属性, 达成不同影响程度的效果, 且可手动或被动修改.
因子链:
    LbFactorChain
    HsFactorChain
        因子链, 维护一个 因子有序列表, 实例化时将初始 State传递.
            调用影响方法, 将 LbState/HsState 在 因子链按序传递, 而每个因子都对状态某些项目进行影响
            返回由所有因子影响后的  LbState/HsState 对象, 即为某股票某时刻 低买高卖仓位决策需要的所有参数.

