package com.atguigu.day03.sink;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class Flink04_Sink_Custom {

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

        result.addSink(new MySink());

        env.execute();
    }
   /* public  static class MySink implements SinkFunction<WaterSensor>{
        @Override
        public void invoke(WaterSensor value, Context context) throws Exception {
            System.out.println("插入一条数据，创建一次连接");
            //1.获取Mysql连接
            Connection connection = DriverManager.getConnection("jdbc:mysql://hadoop102:3306/test?useSSL=false", "root", "123456");

            //2.声明sql语句
            PreparedStatement preparedStatement = connection.prepareStatement("insert into sensor values (?,?,?)");
            //3.给占位符赋值
            preparedStatement.setString(1,value.getId());
            preparedStatement.setLong(2,value.getTs());
            preparedStatement.setInt(3,value.getVc());
            //4.执行赋值操作
            preparedStatement.execute();
            //5.关闭连接
            preparedStatement.close();

            connection.close();
        }
    }*/
   public static class MySink extends RichSinkFunction<WaterSensor>{
       Connection connection;
       PreparedStatement preparedStatement;
       @Override
       public void open(Configuration parameters) throws Exception {
          connection = DriverManager.getConnection("jdbc:mysql://hadoop102:3306/test?useSSL=false", "root", "123456");;
       }

       @Override
       public void invoke(WaterSensor value, Context context) throws Exception {
          preparedStatement = connection.prepareStatement("insert into sensor values (?,?,?)");
           preparedStatement.setString(1,value.getId());
           preparedStatement.setLong(2,value.getTs());
           preparedStatement.setInt(3,value.getVc());

           preparedStatement.execute();
       }

       @Override
       public void close() throws Exception {
           preparedStatement.close();

           connection.close();
       }
   }
}
