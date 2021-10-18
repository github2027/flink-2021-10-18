package com.atguigu.day05.watermark;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public class Flink07_BaseOn_ProcessTime_Timer {
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
        //使用基于处理时间的定时器
        SingleOutputStreamOperator<String> result = keyedStream.process(new KeyedProcessFunction<String, WaterSensor, String>() {
            @Override
            public void processElement(WaterSensor value, Context ctx, Collector<String> out) throws
            Exception {
                //TODO 注册一个，数据过来后，200ms触发的定时器
                ctx.timerService().registerProcessingTimeTimer(ctx.timerService().currentProcessingTime()+200);
            }
            //TODO 触发器触发
            @Override
            public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
                System.out.println("生成waterMark:"+ctx.timerService().currentProcessingTime());
                ctx.timerService().registerProcessingTimeTimer(ctx.timerService().currentProcessingTime()+200);
            }
        });

        result.print();

        env.execute();
    }


}

