package com.atguigu.day03;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Flink01_TransForm_Map {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();

        executionEnvironment.setParallelism(1);

        DataStreamSource<Integer> streamSource = executionEnvironment.fromElements(1, 2, 3, 4, 5);

        SingleOutputStreamOperator<Integer> map = streamSource.map(new MapFunction<Integer, Integer>() {

            @Override
            public Integer map(Integer value) throws Exception {
                return value * 2;
            }
        });

        map.print();
        executionEnvironment.execute();

    }
}
