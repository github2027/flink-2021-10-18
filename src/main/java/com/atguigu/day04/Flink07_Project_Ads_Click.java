package com.atguigu.day04;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.elasticsearch.common.recycler.Recycler;

public class Flink07_Project_Ads_Click {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        DataStreamSource<String> streamSource = env.readTextFile("input/AdClickLog.csv");

        SingleOutputStreamOperator<Tuple2<String, Long>> tuple = streamSource.map(new MapFunction<String, Tuple2<String, Long>>() {
            @Override
            public Tuple2<String, Long> map(String value) throws Exception {
                String[] split = value.split(",");

                AdsClickLog adsClickLog = new AdsClickLog( Long.parseLong(split[0]), Long.parseLong(split[1]), split[2], split[3], Long.parseLong(split[4]));
                return Tuple2.of(adsClickLog.getProvince() + "-" + adsClickLog.getAdId(), 1L);
            }
        });
        KeyedStream<Tuple2<String, Long>, Tuple> keyedStream = tuple.keyBy(0);

        SingleOutputStreamOperator<Tuple2<String, Long>> sum = keyedStream.sum(1);

        sum.print();

        env.execute();
    }
}
