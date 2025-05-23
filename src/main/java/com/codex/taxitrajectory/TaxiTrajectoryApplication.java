package com.codex.taxitrajectory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class TaxiTrajectoryApplication {
	public static void main(String[] args) {
		// 设置应用程序的默认时区为北京时间
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
		SpringApplication.run(TaxiTrajectoryApplication.class, args);
	}
}

