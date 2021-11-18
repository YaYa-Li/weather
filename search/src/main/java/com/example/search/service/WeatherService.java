package com.example.search.service;

import com.example.search.pojo.City;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface WeatherService {
    List<Integer> findCityIdByName(String city);
    Map<String, Map> findCityNameById(int id);
    List<City> getWeatherByName(String cities);
}
