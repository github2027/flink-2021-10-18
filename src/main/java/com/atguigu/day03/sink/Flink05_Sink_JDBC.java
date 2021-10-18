package com.atguigu.day03.sink;

import com.atguigu.day02.WaterSensor;
import com.mysql.jdbc.Driver;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class Flink05_Sink_JDBC {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStreamSource<String> streamSource = env.socketTextStream("hadoop102", 9999);

        SingleOutputStreamOperator<WaterSensor> result = streamSource.map(new MapFunction<String, WaterSensor>() {
            @Override
            public WaterSensor map(String value) throws Exception {

                String[] split = value.split(",");

                WaterSensor waterSensor = new WaterSensor(split[0], Long.parseLong(split[1]), Integer.parseInt(split[2]));
                return waterSensor;
            }
        });

        result.addSink(JdbcSink.sink(
                "insert into sensor values (?,?,?)",
                (ps,t)->{
                    ps.setString(1,t.getId());
                    ps.setLong(2,t.getTs());
                    ps.setInt(3,t.getVc());
                },
                //设置来一条写一条
                new JdbcExecutionOptions.Builder().withBatchSize(1).build(),

                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl("jdbc:mysql://hadoop102:3306/test?useSSL=false")
                        .withUsername("root")
                        .withPassword("123456")
                        .withDriverName(Driver.class.getName())
                        .build()

        ));

        env.execute();
    }

}
