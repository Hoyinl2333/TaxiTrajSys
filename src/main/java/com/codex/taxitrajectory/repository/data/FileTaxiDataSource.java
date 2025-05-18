package com.codex.taxitrajectory.repository.data;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class FileTaxiDataSource {

    private final ResourcePatternResolver resolver;
    private final Map<String, Resource> taxiFiles = new HashMap<>();

    @Value("${taxi.data.path:classpath:data/*.txt}")
    private String path;

    public FileTaxiDataSource(ResourcePatternResolver resolver) {
        this.resolver = resolver;
    }

    @PostConstruct
    public void init() throws IOException {
        for (Resource res : resolver.getResources(path)) {
            String name = res.getFilename();
            if (name != null && name.endsWith(".txt")) {
                taxiFiles.put(name.replace(".txt", ""), res);
            }
        }
    }

    public Resource load(String taxiId) {
        return taxiFiles.get(taxiId);
    }

    public Set<String> listAllTaxiIds() {
        return taxiFiles.keySet();
    }
}
