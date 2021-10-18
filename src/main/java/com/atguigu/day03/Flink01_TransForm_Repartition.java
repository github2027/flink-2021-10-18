package com.atguigu.day03;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Flink01_TransForm_Repartition {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(4);

        DataStreamSource<String> streamSource = env.socketTextStream("hadoop102", 9999);

        SingleOutputStreamOperator<String> map = streamSource.map(r -> r).setParallelism(2);

        KeyedStream<String, String> keyedStream = map.keyBy(r -> r);

        DataStream<String> shuffle = map.shuffle();

        DataStream<String> rebalance = map.rebalance();

        DataStream<String> rescale = map.rescale();

        map.print("original data").setParallelism(2);
        keyedStream.print("keyBy:");
        shuffle.print("shuffle:");
        rebalance.print("rebalance:");
        rescale.print("rescale");

        env.execute();
    }
}
