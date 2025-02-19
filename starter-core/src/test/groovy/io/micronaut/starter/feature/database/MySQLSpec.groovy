package io.micronaut.starter.feature.database

import io.micronaut.starter.ApplicationContextSpec
import io.micronaut.starter.BuildBuilder
import io.micronaut.starter.options.BuildTool
import io.micronaut.starter.options.Language
import spock.lang.Unroll

class MySQLSpec extends ApplicationContextSpec {

    @Unroll
    void 'test gradle mysql feature for language=#language'() {
        when:
        String template = new BuildBuilder(beanContext, BuildTool.GRADLE)
                .features(['mysql'])
                .language(language)
                .render()

        then:
        template.contains('runtimeOnly("mysql:mysql-connector-java")')

        and:
        template.contains("""
    testResources {
        additionalModules.add("jdbc-mysql")
    }""")

        where:
        language << Language.values().toList()
    }

    @Unroll
    void 'test maven mysql feature for language=#language'() {
        when:
        String template = new BuildBuilder(beanContext, BuildTool.MAVEN)
                .features(['mysql'])
                .language(language)
                .render()

        then:
        template.contains("""
    <dependency>
      <groupId>mysql</groupId>
      <artifactId>mysql-connector-java</artifactId>
      <scope>runtime</scope>
    </dependency>
""")

        and:
        template.contains("""
            <testResourcesDependencies>
              <dependency>
                <groupId>io.micronaut.testresources</groupId>
                <artifactId>micronaut-test-resources-jdbc-mysql</artifactId>
              </dependency>
            </testResourcesDependencies>
""")
        where:
        language << Language.values().toList()
    }

}
