package com.atguigu.day04;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.HashSet;

public class Flink04_Project_UV {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        DataStreamSource<String> streamSource = env.readTextFile("input/UserBehavior.csv");

        SingleOutputStreamOperator<UserBehavior> map = streamSource.map(new MapFunction<String, UserBehavior>() {
            @Override
            public UserBehavior map(String value) throws Exception {
                String[] split = value.split(",");

                UserBehavior userBehavior = new UserBehavior(Long.parseLong(split[0]), Long.parseLong(split[1]), Integer.parseInt(split[2]), split[3], Long.parseLong(split[4]));

                return userBehavior;
            }
        });

        SingleOutputStreamOperator<UserBehavior> filter = map.filter(new FilterFunction<UserBehavior>() {
            @Override
            public boolean filter(UserBehavior value) throws Exception {

                return "pv".equals(value.getBehavior());
            }
        });

        SingleOutputStreamOperator<Tuple2<String, Long>>uvTuple = filter.map(new MapFunction<UserBehavior, Tuple2<String, Long>>() {
            @Override
            public Tuple2<String, Long> map(UserBehavior value) throws Exception {

                return Tuple2.of("uv", value.getUserId());
            }
        });

        KeyedStream<Tuple2<String, Long>, Tuple> keyedStream = uvTuple.keyBy(0);


        SingleOutputStreamOperator<Tuple2<String, Long>> uv = keyedStream.process(new KeyedProcessFunction<Tuple, Tuple2<String, Long>, Tuple2<String, Long>>() {
            HashSet<Long> uids = new HashSet<>();

            @Override
            public void processElement(Tuple2<String, Long> value, Context ctx, Collector<Tuple2<String, Long>> out) throws Exception {

                uids.add(value.f1);
                out.collect(Tuple2.of("uv", (long) uids.size()));
            }
        });

        uv.print();

        env.execute();
    }

}
