package com.atguigu.day1;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.*;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

public class Fink_Batch {
    public static void main(String[] args) throws Exception {
        ExecutionEnvironment executionEnvironment = ExecutionEnvironment.getExecutionEnvironment();

        DataSource<String> stringDataSource = executionEnvironment.readTextFile("input/word.txt");

        FlatMapOperator<String, String> step1 = stringDataSource.flatMap(new FlatMapFunction<String, String>() {
            @Override
            public void flatMap(String value, Collector<String> out) throws Exception {
                String[] strings = value.split(" ");

                for (String string : strings) {
                    out.collect(string);
                }
            }
        });
        MapOperator<String, Tuple2<String, Long>> step2 = step1.map(new MapFunction<String, Tuple2<String, Long>>() {
            @Override
            public Tuple2<String, Long> map(String value) throws Exception {
                return new Tuple2<String, Long>(value, 1L);
            }
        });

        UnsortedGrouping<Tuple2<String, Long>> step3 = step2.groupBy(0);

        AggregateOperator<Tuple2<String, Long>> step4 = step3.sum(1);

        step4.print();
    }
}
