package com.atguigu.day04;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.util.Collector;

import java.util.HashMap;

public class Flink08_Project_Order {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        DataStreamSource<String> orderStream = env.readTextFile("input/OrderLog.csv");

        DataStreamSource<String> txStream = env.readTextFile("input/ReceiptLog.csv");

        SingleOutputStreamOperator<OrderEvent> orderEventStream = orderStream.map(new MapFunction<String, OrderEvent>() {
            @Override
            public OrderEvent map(String value) throws Exception {
                String[] split = value.split(",");

                return new OrderEvent(
                        Long.parseLong(split[0]),
                        split[1],
                        split[2],
                        Long.parseLong(split[3])
                );
            }
        });
        SingleOutputStreamOperator<TxEvent> txEventStream = txStream.map(new MapFunction<String, TxEvent>() {
            @Override
            public TxEvent map(String value) throws Exception {
                String[] split = value.split(",");
                return new TxEvent(split[0], split[1], Long.parseLong(split[2]));
            }
        });

        ConnectedStreams<OrderEvent, TxEvent> connect = orderEventStream.connect(txEventStream);

        ConnectedStreams<OrderEvent, TxEvent> orderEventTxEventConnectedStreams = connect.keyBy("txId", "txId");

        orderEventTxEventConnectedStreams.process(new KeyedCoProcessFunction<String, OrderEvent, TxEvent, String>() {
            HashMap<String, OrderEvent> orderEventHashMap = new HashMap<>();
            HashMap<String, TxEvent> txEventHashMap = new HashMap<>();
            @Override
            public void processElement1(OrderEvent value, Context ctx, Collector<String> out) throws Exception {

                if(txEventHashMap.containsKey(value.getTxId())){
                    out.collect("订单："+value.getOrderId()+"对账成功！！！");
                    txEventHashMap.remove(value.getTxId());
                }else {
                    orderEventHashMap.put(value.getTxId(),value);
                }
            }

            @Override
            public void processElement2(TxEvent value, Context ctx, Collector<String> out) throws Exception {
                    if(orderEventHashMap.containsKey(value.getTxId())){
                        out.collect("订单："+orderEventHashMap.get(value.getTxId()).getOrderId()+"对账成功！！！");
                        orderEventHashMap.remove(value.getTxId());
                    }else {
                        txEventHashMap.put(value.getTxId(),value);
                    }
            }
        }).print();
        env.execute();
    }
}
