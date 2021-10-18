package com.atguigu.day03;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Flink01_TransForm_Filter {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
        executionEnvironment.setParallelism(1);

        DataStreamSource<Integer> integerDataStreamSource = executionEnvironment.fromElements(10, 3, 5, 9, 20, 8);

        SingleOutputStreamOperator<Integer> filter = integerDataStreamSource.filter(new FilterFunction<Integer>() {
            @Override
            public boolean filter(Integer value) throws Exception {
                return value > 5;
            }
        });

        filter.print();
        executionEnvironment.execute();
    }
}
