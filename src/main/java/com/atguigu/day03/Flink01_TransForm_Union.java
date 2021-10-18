package com.atguigu.day03;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Flink01_TransForm_Union {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStreamSource<Integer> stream1 = env.fromElements(1, 2, 3, 4, 5);
        DataStreamSource<Integer> stream2 = env.fromElements(10, 20, 30, 40, 50);
        DataStreamSource<Integer> stream3 = env.fromElements(100, 200, 300, 400, 500);

        DataStream<Integer> union = stream1.union(stream2).union(stream3);

        union.print("union");

        env.execute();

    }
}
