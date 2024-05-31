package com.ncslzh.projects.config;

public interface ConfigService {

    <T> T of(String configName, T defaultValue, Class<T> clazz);
}
