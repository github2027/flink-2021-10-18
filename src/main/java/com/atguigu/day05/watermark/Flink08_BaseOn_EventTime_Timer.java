package com.atguigu.day05.watermark;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.time.Duration;

public class Flink08_BaseOn_EventTime_Timer {
    public static void main(String[] args) throws Exception {
        //StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
        env.setParallelism(1);

        DataStreamSource<String> streamSource = env.socketTextStream("hadoop102", 9999);

        SingleOutputStreamOperator<WaterSensor> waterSensorStream = streamSource.map(new MapFunction<String, WaterSensor>() {
            @Override
            public WaterSensor map(String value) throws Exception {
                String[] split = value.split(",");
                return new WaterSensor(split[0], Long.parseLong(split[1]), Integer.parseInt(split[2]));
            }
        });
        SingleOutputStreamOperator<WaterSensor> watermarks = waterSensorStream.assignTimestampsAndWatermarks(WatermarkStrategy.<WaterSensor>forBoundedOutOfOrderness(Duration.ofSeconds(2))
                .withTimestampAssigner(new SerializableTimestampAssigner<WaterSensor>() {
                    @Override
                    public long extractTimestamp(WaterSensor element, long recordTimestamp) {
                        return element.getTs() * 1000;
                    }
                })
        );


        KeyedStream<WaterSensor, String> watermak = watermarks.keyBy(r -> r.getId());
        //使用基于事件时间的定时器
        SingleOutputStreamOperator<String> result = watermak.process(new KeyedProcessFunction<String, WaterSensor, String>() {
            @Override
            public void processElement(WaterSensor value, Context ctx, Collector<String> out) throws
            Exception {
                //TODO 注册一个时间为200ms的基于事件时间的定时器->timestamp第一条数据里记录的时间+5000->当watermark达到上一轮的时间则触发(右边是闭区间)
                //TODO 由于WaterMark是贯穿整个时间线的，不管是哪个key,waterMark到了就可以触发定时器。
                System.out.println("注册一个定时器"+ctx.getCurrentKey());
                ctx.timerService().registerEventTimeTimer( ctx.timestamp()+5000);
            }
            //TODO 触发器触发
            @Override
            public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
                out.collect("定时器被触发。。。。"+ctx.getCurrentKey());
            }
        });

        result.print();

        env.execute();
    }


}

