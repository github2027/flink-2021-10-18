package com.atguigu.day04;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Flink02_Project_PV {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<String> streamSource = env.readTextFile("input/UserBehavior.csv");
        SingleOutputStreamOperator<UserBehavior> userBehaviorSingleOutputStreamOperator = streamSource.map(new MapFunction<String, UserBehavior>() {
            @Override
            public UserBehavior map(String value) throws Exception {
                String[] split = value.split(",");

                UserBehavior userBehavior = new UserBehavior(Long.parseLong(split[0]), Long.parseLong(split[1]), Integer.parseInt(split[2]), split[3], Long.parseLong(split[4]));
                return userBehavior;
            }
        });

        SingleOutputStreamOperator<UserBehavior> psStream = userBehaviorSingleOutputStreamOperator.filter(new FilterFunction<UserBehavior>() {
            @Override
            public boolean filter(UserBehavior value) throws Exception {

                return "pv".equals(value.getBehavior());
            }
        });

        SingleOutputStreamOperator<Tuple2<String, Integer>> tupleStream = psStream.map(new MapFunction<UserBehavior, Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> map(UserBehavior value) throws Exception {

                return Tuple2.of(value.getBehavior(), 1);
            }
        });

        KeyedStream<Tuple2<String, Integer>, Tuple> keyedStream = tupleStream.keyBy(0);

        SingleOutputStreamOperator<Tuple2<String, Integer>> result = keyedStream.sum(1);

        result.print();

        env.execute();
    }
}
