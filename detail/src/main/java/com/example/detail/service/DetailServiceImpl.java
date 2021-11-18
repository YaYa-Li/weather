package com.example.detail.service;

import com.example.detail.pojo.City;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service
public class DetailServiceImpl implements DetailService{


    private final RestTemplate restTemplate;

    @Autowired
    public DetailServiceImpl(RestTemplate getRestTemplate) {
        this.restTemplate = getRestTemplate;
    }


    @Override
    @Retryable(include = IllegalAccessError.class )
    public Integer getCityIdByName(String city) {
        System.out.println("detail service: city = "+city);
        City[] city_obj = restTemplate.getForObject("https://www.metaweather.com/api/location/search/?query=" + city, City[].class);

        if(city_obj.length>0){
            int id = city_obj[0].getWoeid();
            System.out.println("detail service ididid = "+id);
            return id;
        }else{
            return 0;
        }








    }
}
