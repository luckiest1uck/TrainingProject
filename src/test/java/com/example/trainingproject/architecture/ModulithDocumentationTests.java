package com.example.trainingproject.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import com.example.trainingproject.TrainingProjectApplication;

class ModulithDocumentationTests {

    private final ApplicationModules modules = ApplicationModules.of(
            TrainingProjectApplication.class, ApplicationModules.Filters.withoutModule("openapi"));

    @Test
    void writeModuleDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
