package com.atguigu.day03;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.util.stream.Stream;

public class Flink01_TransForm_FlatMap {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();

        executionEnvironment.setParallelism(2);

        DataStreamSource<String> stringDataStreamSource = executionEnvironment.readTextFile("input/word.txt");

       /* SingleOutputStreamOperator<String> flatMap = stringDataStreamSource.flatMap(new FlatMapFunction<String, String>() {
            @Override
            public void flatMap(String value, Collector<String> out) throws Exception {
                String[] words = value.split(" ");

                for (String word : words) {
                    out.collect(word);
                }
            }
        });*/
        SingleOutputStreamOperator<String> flatMap = stringDataStreamSource.flatMap(new MyFlatMap());


        flatMap.print();

        executionEnvironment.execute();
    }

    //自定义一个类继承富函数
    public static class MyFlatMap extends RichFlatMapFunction<String,String>{
        @Override
        public void open(Configuration parameters) throws Exception {
            System.out.println("open....");
        }

        @Override
        public void flatMap(String value, Collector<String> out) throws Exception {
            String[] words = value.split(" ");
            System.out.println(getRuntimeContext().getTaskNameWithSubtasks());
            for (String word : words) {
                out.collect(word);
            }
        }

        @Override
        public void close() throws Exception {
            System.out.println("close....");
        }
    }
}
