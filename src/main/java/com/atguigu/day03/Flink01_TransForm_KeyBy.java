package com.atguigu.day03;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Flink01_TransForm_KeyBy {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
        executionEnvironment.setParallelism(2);

        DataStreamSource<Integer> integerDataStreamSource = executionEnvironment.fromElements(10, 3, 5, 9, 20, 8);

        KeyedStream<Integer, String> integerStringKeyedStream = integerDataStreamSource.keyBy(new KeySelector<Integer, String>() {
            @Override
            public String getKey(Integer value) throws Exception {
                return value % 2 == 0 ? "偶数" : "奇数";
            }
        });

        integerStringKeyedStream.print();


        executionEnvironment.execute();
    }
}
