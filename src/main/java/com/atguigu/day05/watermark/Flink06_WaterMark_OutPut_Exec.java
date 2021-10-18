package com.atguigu.day05.watermark;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Duration;

public class Flink06_WaterMark_OutPut_Exec {
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
        KeyedStream<WaterSensor, String> keyedStream = waterSensorStream.keyBy(r -> r.getId());
        //采集监控传感器水位值，将水位值高于5cm的值输出到side output
        SingleOutputStreamOperator<WaterSensor> result = keyedStream.process(new KeyedProcessFunction<String, WaterSensor, WaterSensor>() {
            @Override
            public void processElement(WaterSensor value, Context ctx, Collector<WaterSensor> out) throws Exception {
                if (value.getVc() > 5) {
                    ctx.output(new OutputTag<WaterSensor>("output"){}, value);
                }
                out.collect(value);
            }
        });

        result.print("Main");
        DataStream<WaterSensor> sideOutput = result.getSideOutput(new OutputTag<WaterSensor>("output") {
        });
        sideOutput.print("sideOutput->水位高于5cm");
        env.execute();
    }


}

