package com.ncslzh.projects.placeholders;

public interface ConfigService {

    <T> T of(String configName, T defaultValue, Class<T> clazz);
}
