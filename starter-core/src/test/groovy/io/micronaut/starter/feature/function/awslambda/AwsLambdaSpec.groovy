package io.micronaut.starter.feature.function.awslambda

import io.micronaut.starter.ApplicationContextSpec
import io.micronaut.starter.BuildBuilder
import io.micronaut.starter.application.ApplicationType
import io.micronaut.starter.feature.MicronautRuntimeFeature
import io.micronaut.starter.feature.build.gradle.Gradle
import io.micronaut.starter.feature.coherence.CoherenceFeature
import io.micronaut.starter.fixture.CommandOutputFixture
import io.micronaut.starter.options.*
import spock.lang.Shared
import spock.lang.Subject
import spock.lang.Unroll

class AwsLambdaSpec extends ApplicationContextSpec implements CommandOutputFixture {

    @Shared
    @Subject
    AwsLambda awsLambda = beanContext.getBean(AwsLambda)

    void 'test gradle.properties does not contain micronaut.runtime'() {
        when:
        Map<String, String> output = generate(['aws-lambda'])
        String properties = output["gradle.properties"]

        then:
        properties
        !properties.contains(MicronautRuntimeFeature.PROPERTY_MICRONAUT_RUNTIME)
        output["build.gradle"].contains('runtime("lambda_java")')
    }

    void 'test readme.md with feature aws-lambda contains links to micronaut docs'() {
        when:
        def output = generate(['aws-lambda'])
        def readme = output["README.md"]

        then:
        readme
        readme.contains("https://micronaut-projects.github.io/micronaut-aws/latest/guide/index.html#lambda")
    }

    void 'test readme.md for application #applicationType feature aws-lambda contains Handler '(ApplicationType applicationType,
                                                                                                String handler) {
        when:
        def output = generate(applicationType, ['aws-lambda'])
        def readme = output["README.md"]

        then:
        readme
        readme.contains("[AWS Lambda Handler](https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html)")
        readme.contains("Handler: $handler".toString())

        where:
        applicationType             | handler
        ApplicationType.DEFAULT     | 'io.micronaut.function.aws.proxy.MicronautLambdaHandler'
        ApplicationType.FUNCTION    | 'example.micronaut.FunctionRequestHandler'
    }

    @Unroll
    void "aws-lambda does not support #description"(ApplicationType applicationType, String description) {
        expect:
        !awsLambda.supports(applicationType)

        where:
        applicationType << ApplicationType.values().toList() -  [ApplicationType.DEFAULT, ApplicationType.FUNCTION]
        description = applicationType.name
    }

    void "aws-lambda supports function application type"(ApplicationType applicationType) {
        expect:
        awsLambda.supports(applicationType)

        where:
        applicationType << [ApplicationType.DEFAULT, ApplicationType.FUNCTION]
    }

    @Unroll
    void 'aws-lambda is the default feature for function for gradle and language=#language'(Language language) {
        when:
        String template = new BuildBuilder(beanContext, BuildTool.GRADLE)
                .language(language)
                .applicationType(ApplicationType.FUNCTION)
                .render()

        then:
        template.contains('implementation("io.micronaut.aws:micronaut-function-aws")')

        where:
        language << Language.values()
    }

    @Unroll
    void 'test gradle aws-lambda feature for language=#language'(Language language) {
        when:
        String template = new BuildBuilder(beanContext, BuildTool.GRADLE)
                .features(['aws-lambda'])
                .language(language)
                .applicationType(ApplicationType.FUNCTION)
                .render()

        then:
        template.contains('implementation("io.micronaut.aws:micronaut-function-aws")')

        where:
        language << Language.values()
    }

    @Unroll
    void 'aws-lambda feature is default feature for function and language=#language'(Language language) {
        when:
        String template = new BuildBuilder(beanContext, BuildTool.MAVEN)
                .language(language)
                .applicationType(ApplicationType.FUNCTION)
                .render()

        then:
        template.contains("""
    <dependency>
      <groupId>io.micronaut.aws</groupId>
      <artifactId>micronaut-function-aws</artifactId>
      <scope>compile</scope>
    </dependency>
""")
        and:
        template.contains('''\
      <plugin>
        <groupId>io.micronaut.build</groupId>
        <artifactId>micronaut-maven-plugin</artifactId>
      </plugin>
''')

        where:
        language << Language.values()
    }

    @Unroll
    void 'function with maven and aws-lambda feature for language=#language'(Language language) {
        when:
        String template = new BuildBuilder(beanContext, BuildTool.MAVEN)
                .language(language)
                .applicationType(ApplicationType.FUNCTION)
                .features(['aws-lambda'])
                .render()

        then:
        template.contains("""
    <dependency>
      <groupId>io.micronaut.aws</groupId>
      <artifactId>micronaut-function-aws</artifactId>
      <scope>compile</scope>
    </dependency>
""")

        where:
        language << Language.values()
    }

    @Unroll
    void 'function with gradle and feature aws-lambda for language=#language'() {
        when:
        String build = new BuildBuilder(beanContext, BuildTool.GRADLE)
                .language(language)
                .applicationType(ApplicationType.FUNCTION)
                .features(['aws-lambda'])
                .render()

        then:
        build.contains('implementation("io.micronaut.aws:micronaut-function-aws")')
        !build.contains('implementation "io.micronaut:micronaut-http-server-netty"')
        !build.contains('implementation "io.micronaut:micronaut-http-client"')

        and:
        build.contains('runtime("lambda_java")')

        where:
        language << Language.values().toList()
    }

    void 'function with gradle, with features aws-lambda and graalvm for language=#language'() {
        when:
        String build = new BuildBuilder(beanContext, BuildTool.GRADLE)
                .language(language)
                .applicationType(ApplicationType.FUNCTION)
                .features(['aws-lambda', 'graalvm'])
                .render()

        then:
        build.contains('implementation("io.micronaut.aws:micronaut-function-aws")')
        !build.contains('implementation "io.micronaut:micronaut-http-server-netty"')
        !build.contains('implementation "io.micronaut:micronaut-http-client"')

        and:
        build.contains('runtime("lambda_provided")')

        where:
        // Graalvm feature doesn't work with Groovy
        language << Language.values().toList() - Language.GROOVY
    }

    @Unroll
    void 'files with gradle and feature aws-lambda for language=#language'() {
        when:
        def output = generate(
                ApplicationType.FUNCTION,
                new Options(language, BuildTool.GRADLE),
                ['aws-lambda']
        )

        then:
        output.containsKey("$srcDir/example/micronaut/FunctionRequestHandler.$extension".toString())
        output.containsKey(language.getDefaults().getTest().getSourcePath("/example/micronaut/FunctionRequestHandler", language))

        where:
        language << Language.values().toList()
        extension << Language.extensions()
        srcDir << Language.srcDirs()
        testSrcDir << Language.testSrcDirs()
    }

    @Unroll
    void 'function with maven and feature aws-lambda for language=#language'() {
        when:
        def output = generate(
                ApplicationType.FUNCTION,
                new Options(language, BuildTool.MAVEN),
                ['aws-lambda']
        )
        String build = output['pom.xml']

        then:
        build.contains('<artifactId>micronaut-function-aws</artifactId>')
        !build.contains('<exec.mainClass>')
        !build.contains('</exec.mainClass>')
        !build.contains('<artifactId>micronaut-http-server-netty</artifactId>')

        output.containsKey("$srcDir/example/micronaut/FunctionRequestHandler.$extension".toString())
        output.containsKey(language.getDefaults().getTest().getSourcePath("/example/micronaut/FunctionRequestHandler", language))

        where:
        language << Language.values().toList()
        extension << Language.extensions()
        srcDir << Language.srcDirs()
        testSrcDir << Language.testSrcDirs()
    }

    @Unroll
    void 'Application file is generated for a default application type with gradle and features aws-lambda for language: #language'(Language language, String extension) {
        when:
        def output = generate(
                ApplicationType.DEFAULT,
                new Options(language, BuildTool.GRADLE),
                ['aws-lambda']
        )

        then:
        output.containsKey("${language.srcDir}/example/micronaut/Application.${extension}".toString())

        when:
        def buildGradle = output['build.gradle']

        then:
        !buildGradle.contains('id "application"')
        buildGradle.contains('mainClass.set')
        buildGradle.contains('id("io.micronaut.application")')

        buildGradle.contains('''\
tasks.named("dockerfileNative") {
    args(
        "-XX:MaximumHeapSizePercent=80",
        "-Dio.netty.allocator.numDirectArenas=0",
        "-Dio.netty.noPreferDirect=true"
    )
}''')
        where:
        language << Language.values().toList()
        extension << Language.extensions()
    }

    void "kotlin.kapt plugin is applied before micronaut library plugin"() {
        when:
        Map<String, String> output = generate(
                ApplicationType.FUNCTION,
                new Options(Language.KOTLIN, BuildTool.GRADLE_KOTLIN),
                ['aws-lambda']
        )
        String buildGradle = output['build.gradle.kts']

        then:
        buildGradle.contains('org.jetbrains.kotlin.kapt')
        buildGradle.contains('io.micronaut.library')
        buildGradle.indexOf('org.jetbrains.kotlin.kapt') < buildGradle.indexOf('io.micronaut.library')
    }

    @Unroll
    void 'Application file is generated for a default application type with gradle Kotlin DSL and features aws-lambda for language: #language'(Language language, String extension) {
        when:
        def output = generate(
                ApplicationType.DEFAULT,
                new Options(language, BuildTool.GRADLE_KOTLIN),
                ['aws-lambda']
        )

        then:
        output.containsKey("${language.srcDir}/example/micronaut/Application.${extension}".toString())

        when:
        String buildGradle = output['build.gradle.kts']

        then:
        !buildGradle.contains('id "application"')
        buildGradle.contains('mainClass.set')
        buildGradle.contains('id("io.micronaut.application")')

        buildGradle.contains('''\
tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    args(
        "-XX:MaximumHeapSizePercent=80",
        "-Dio.netty.allocator.numDirectArenas=0",
        "-Dio.netty.noPreferDirect=true"
    )
}''')
        where:
        language << Language.values().toList()
        extension << Language.extensions()
    }

    @Unroll
    void 'Application file is generated for a default application type with gradle and features aws-lambda and graalvm for language: #language'(Language language, String extension) {
        when:
        def output = generate(
                ApplicationType.DEFAULT,
                new Options(language, TestFramework.JUNIT, BuildTool.GRADLE, JdkVersion.JDK_11),
                ['aws-lambda', 'graalvm']
        )

        then:
        output.containsKey("${language.srcDir}/example/micronaut/Application.${extension}".toString())

        where:
        language << graalSupportedLanguages()
        extension << graalSupportedLanguages()*.extension
    }

    @Unroll
    void 'Application file is generated for a default application type with gradle and features aws-lambda and aws-lambda-custom-runtime for language: #language'(Language language, String extension) {
        when:
        def output = generate(
                ApplicationType.DEFAULT,
                new Options(language, TestFramework.JUNIT, BuildTool.GRADLE, JdkVersion.JDK_11),
                ['aws-lambda', 'aws-lambda-custom-runtime']
        )

        then:
        !output.containsKey("${language.srcDir}/example/micronaut/Application.${extension}".toString())

        when:
        def buildGradle = output['build.gradle']

        then:
        buildGradle.contains('mainClass.set("io.micronaut.function.aws.runtime.MicronautLambdaRuntime")')

        where:
        language << Language.values().toList()
        extension << Language.extensions()
    }

    @Unroll
    void 'aws-lambda features includes dependency to micronaut-function-aws-api-proxy for function for gradle and language=#language'(Language language) {
        when:
        String template = new BuildBuilder(beanContext, BuildTool.GRADLE)
                .features(['aws-lambda'])
                .language(language)
                .render()

        then:
        template.contains('runtime("lambda_java")')
        !template.contains('implementation("io.micronaut:micronaut-http-server-netty")')
        !template.contains('implementation("io.micronaut:micronaut-http-client")')

        where:
        language << Language.values()
    }

    void 'aws-lambda features includes dependency to micronaut-function-aws-api-proxy for function for gradle and language=#language with graalvm'(Language language) {
        when:
        String template = new BuildBuilder(beanContext, BuildTool.GRADLE)
                .features(['aws-lambda', 'graalvm'])
                .language(language)
                .render()

        then:
        template.contains('runtime("lambda_provided")')
        !template.contains('implementation("io.micronaut:micronaut-http-server-netty")')
        !template.contains('implementation("io.micronaut:micronaut-http-client")')

        where:
        // Graalvm feature doesn't work with Groovy
        language << Language.values() - Language.GROOVY
    }

    @Unroll
    void 'test maven micronaut-function-aws-api-proxy feature for language=#language'(Language language) {
        when:
        String template = new BuildBuilder(beanContext, BuildTool.MAVEN)
                .language(language)
                .features(['aws-lambda'])
                .render()

        then:
        template.contains("""
    <dependency>
      <groupId>io.micronaut.aws</groupId>
      <artifactId>micronaut-function-aws-api-proxy</artifactId>
      <scope>compile</scope>
    </dependency>
""")

        where:
        language << Language.values()
    }

    @Unroll
    void 'app with gradle and feature aws-lambda and graalvm applies aws-lambda-custom-runtime for language=#language'(
            ApplicationType applicationType,
            Language language
    ) {
        when:
        def output = generate(
                applicationType,
                new Options(language, TestFramework.JUNIT, BuildTool.GRADLE, JdkVersion.JDK_11),
                ['aws-lambda', 'graalvm']
        )
        String build = output['build.gradle']

        then:
        build.contains('runtime("lambda_provided")')
        if (applicationType == ApplicationType.DEFAULT) {
            assert !build.contains('implementation("io.micronaut.aws:micronaut-function-aws-custom-runtime")')
        } else if (applicationType == ApplicationType.FUNCTION) {
            assert build.contains('implementation("io.micronaut.aws:micronaut-function-aws-custom-runtime")')
        }
        !build.contains('implementation "io.micronaut:micronaut-http-server-netty"')
        !build.contains('implementation "io.micronaut:micronaut-http-client"')

        where:
        applicationType << [ApplicationType.DEFAULT, ApplicationType.FUNCTION]
        language << graalSupportedLanguages()
    }

    private List<Language> graalSupportedLanguages() {
        Language.values().toList() - Language.GROOVY
    }

    @Unroll
    void 'app with maven and feature aws-lambda does not apply aws-lambda-custom-runtime for language=#language'() {
        when:
        def output = generate(
                ApplicationType.DEFAULT,
                new Options(language, TestFramework.JUNIT, BuildTool.MAVEN),
                ['aws-lambda']
        )
        String build = output['pom.xml']

        then:
        !build.contains('<artifactId>micronaut-function-aws-custom-runtime</artifactId>')
        build.contains('<exec.mainClass>')
        build.contains('</exec.mainClass>')
        !build.contains('<artifactId>micronaut-http-server-netty</artifactId>')
        build.contains('<artifactId>micronaut-http-client</artifactId>')
        build.contains('<artifactId>micronaut-function-aws-api-proxy</artifactId>')

        where:
        language << Language.values().toList()
    }

    @Unroll
    void 'app with maven and feature aws-lambda and graalvm applies aws-lambda-custom-runtime for language=#language'() {
        when:
        def output = generate(
                ApplicationType.DEFAULT,
                new Options(language, TestFramework.JUNIT, BuildTool.MAVEN, JdkVersion.JDK_11),
                ['aws-lambda', 'graalvm']
        )
        String build = output['pom.xml']

        then:
        build.contains('<exec.mainClass>io.micronaut.function.aws.runtime.MicronautLambdaRuntime</exec.mainClass>')
        build.contains('<artifactId>micronaut-function-aws-api-proxy</artifactId>')
        build.contains('<artifactId>micronaut-function-aws-custom-runtime</artifactId>')
        !build.contains('<artifactId>micronaut-http-server-netty</artifactId>')
        build.contains('<artifactId>micronaut-http-client</artifactId>')

        where:
        language << graalSupportedLanguages()
    }

    @Unroll
    void 'app with gradle and feature aws-lambda for language=#language'() {
        when:
        def output = generate(
                ApplicationType.DEFAULT,
                new Options(language, BuildTool.GRADLE),
                ['aws-lambda']
        )
        String build = output['build.gradle']

        then:
        build.contains('runtime("lambda_java")')
        !build.contains('implementation "io.micronaut:micronaut-http-server-netty"')
        !build.contains('implementation "io.micronaut:micronaut-http-client"')

        output.containsKey("$srcDir/example/micronaut/HomeController.$extension".toString())
        output.containsKey(language.getDefaults().getTest().getSourcePath("/example/micronaut/HomeController", language))

        where:
        language << Language.values().toList()
        extension << Language.extensions()
        srcDir << Language.srcDirs()
        testSrcDir << Language.testSrcDirs()
    }

    @Unroll
    void 'app with maven and feature aws-lambda for language=#language'() {
        when:
        def output = generate(
                ApplicationType.DEFAULT,
                new Options(language, BuildTool.MAVEN),
                ['aws-lambda']
        )
        String build = output['pom.xml']

        then:
        build.contains('<artifactId>micronaut-function-aws-api-proxy</artifactId>')
        !build.contains('<artifactId>micronaut-http-server-netty</artifactId>')
        output.containsKey("$srcDir/example/micronaut/HomeController.$extension".toString())
        output.containsKey(language.getDefaults().getTest().getSourcePath("/example/micronaut/HomeController", language))

        where:
        language << Language.values().toList()
        extension << Language.extensions()
        srcDir << Language.srcDirs()
        testSrcDir << Language.testSrcDirs()
    }
}
