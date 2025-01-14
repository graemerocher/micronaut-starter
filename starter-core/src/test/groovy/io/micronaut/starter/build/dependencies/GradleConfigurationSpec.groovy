package io.micronaut.starter.build.dependencies

import io.micronaut.starter.build.gradle.GradleConfiguration
import io.micronaut.starter.options.Language
import io.micronaut.starter.options.TestFramework
import spock.lang.Specification
import spock.lang.Unroll

class GradleConfigurationSpec extends Specification {
    @Unroll
    void "GradleConfiguration::toString() returns the gradle configuration"(GradleConfiguration gradleConfiguration, String expected) {
        expect:
        expected == gradleConfiguration.toString()

        where:
        expected                  | gradleConfiguration
        'runtimeOnly'             | GradleConfiguration.RUNTIME_ONLY
        'developmentOnly'         | GradleConfiguration.DEVELOPMENT_ONLY
        'testImplementation'      | GradleConfiguration.TEST_IMPLEMENTATION
        'implementation'          | GradleConfiguration.IMPLEMENTATION
        'runtimeOnly'             | GradleConfiguration.RUNTIME_ONLY
        'compileOnly'             | GradleConfiguration.COMPILE_ONLY
        'testCompileOnly'         | GradleConfiguration.TEST_COMPILE_ONLY
        'kapt'                    | GradleConfiguration.KAPT
        'kaptTest'                | GradleConfiguration.TEST_KAPT
        'rewrite'                 |  GradleConfiguration.OPENREWRITE
        'annotationProcessor'     | GradleConfiguration.ANNOTATION_PROCESSOR
        'testAnnotationProcessor' | GradleConfiguration.TEST_ANNOTATION_PROCESSOR
        'api'                     |GradleConfiguration.API
    }

    @Unroll("#description")
    void "it is possible to adapt from source and phases to Gradle configuration"(Source source,
                                                                                  List<Phase> phases, GradleConfiguration configuration,
                                                                                  String description) {
        expect:
        configuration == GradleConfiguration.of(new Scope(source, phases), Language.JAVA, TestFramework.JUNIT).get()

        where:
        source      | phases                              || configuration
        Source.MAIN | [Phase.DEVELOPMENT]                 || GradleConfiguration.DEVELOPMENT_ONLY
        Source.MAIN | [Phase.RUNTIME, Phase.COMPILATION]  || GradleConfiguration.IMPLEMENTATION
        Source.MAIN | [Phase.RUNTIME]                     || GradleConfiguration.RUNTIME_ONLY
        Source.TEST | [Phase.RUNTIME]                     || GradleConfiguration.TEST_RUNTIME_ONLY
        Source.MAIN | [Phase.COMPILATION]                 || GradleConfiguration.COMPILE_ONLY
        Source.TEST | [Phase.COMPILATION]                 || GradleConfiguration.TEST_COMPILE_ONLY
        Source.TEST | [Phase.RUNTIME, Phase.COMPILATION]  || GradleConfiguration.TEST_IMPLEMENTATION
        Source.MAIN | [Phase.ANNOTATION_PROCESSING]       || GradleConfiguration.ANNOTATION_PROCESSOR
        Source.TEST | [Phase.ANNOTATION_PROCESSING]       || GradleConfiguration.TEST_ANNOTATION_PROCESSOR
        description = "$source ${phases.join(",")} should return ${configuration.toString()}"
    }
}
