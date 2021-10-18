package com.atguigu.day03;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Flink01_TransForm_Shuffle {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();

        executionEnvironment.setParallelism(2);

        DataStreamSource<Integer> integerDataStreamSource = executionEnvironment.fromElements(10, 3, 5, 9, 20, 8);

        integerDataStreamSource.shuffle().print();

        executionEnvironment.execute();
    }
}
