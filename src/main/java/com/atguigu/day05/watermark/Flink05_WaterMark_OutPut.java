package com.atguigu.day05.watermark;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Duration;

public class Flink05_WaterMark_OutPut {
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

        //TODO 尽量在KeyBy之前获取WaterMark，如果在KeyBy之后，获取了WaterMark，可能无法开窗。
        //TODO 使用时间事件并使用watermark 并指定WaterMark 设置乱序程度的WaterMark,指定数据中哪个字段作为事件时间戳。

        SingleOutputStreamOperator<WaterSensor> waterSensorSingleOutputStreamOperator = waterSensorStream.assignTimestampsAndWatermarks(WatermarkStrategy
                .<WaterSensor>forBoundedOutOfOrderness(Duration.ofSeconds(2))
                .withTimestampAssigner(new SerializableTimestampAssigner<WaterSensor>() {
                    @Override
                    public long extractTimestamp(WaterSensor element, long recordTimestamp) {

                        return element.getTs() * 1000;
                    }
                })
        );

        KeyedStream<WaterSensor, String> keyedStream = waterSensorSingleOutputStreamOperator.keyBy(r->r.getId());

        //开启一个窗口大小为5S的滚动窗口
        WindowedStream<WaterSensor, String, TimeWindow> window = keyedStream.window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .allowedLateness(Time.seconds(3))
                //侧输出流(将迟到的数据输出到侧输出流中)
                .sideOutputLateData(new OutputTag<WaterSensor>("output->inner class"){})
        ;

        SingleOutputStreamOperator<String> result = window.process(new ProcessWindowFunction<WaterSensor, String, String, TimeWindow>() {
            @Override
            public void process(String key, Context context, Iterable<WaterSensor> elements, Collector<String> out) throws Exception {
                String msg = "当前key: " + key
                        + "窗口: [" + context.window().getStart() / 1000 + "," + context.window().getEnd() / 1000 + ") 一共有 "
                        + elements.spliterator().estimateSize() + "条数据 ";
                out.collect(msg);
            }
        });
        result.print("MainOutPut");
        DataStream<WaterSensor> sideOutput = result.getSideOutput(new OutputTag<WaterSensor>("output->inner class") {
        });
        sideOutput.print("SideOutPut");
        env.execute();
    }


}

