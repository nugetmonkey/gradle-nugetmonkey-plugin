package com.nugetmonkey

import org.gradle.api.tasks.bundling.Jar
import org.junit.Before

import static org.junit.Assert.*
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class IkvmPluginTest {
    @Test
    public void ikvmPluginAddsIkvmTasksToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'ikvm'
        assertTrue(project.tasks.ikvm instanceof Ikvm)
        assertTrue(project.tasks.ikvmDoc instanceof IkvmDoc)
        project.ikvm {
            ikvmHome = 'abc'
        }
        assertEquals('abc', project.tasks.ikvm.ikvmHome)
    }

    @Test
    public void ensureIkvmTaskdependsOnJarByDefault() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'ikvm'
        project.evaluate()
        assertTrue(project.ikvm.dependsOn.contains(project.jar))
    }

    @Test
    public void shouldDependOnOtherTaskWhenTargetingAnotherJar() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'ikvm'
        project.task('otherJar', type: Jar) {
            baseName = 'foo'
        }
        project.ikvm {
            jars = [ project.otherJar.archivePath ]
        }
        project.evaluate()
        assertFalse(project.ikvm.dependsOn.contains(project.jar))
        assertTrue(project.ikvm.dependsOn.contains(project.otherJar))
    }

    @Test
    public void commandLineContainsMonkeyJar() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'NugetMonkey'
        def cmd = project.NugetMonkey.commandLineArgs
        assertTrue(cmd.contains(project.jar.archivePath))
    }

    @Test
    public void commandLineContainsJar() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'ikvm'
        def cmd = project.ikvm.commandLineArgs
        assertTrue(cmd.contains(project.jar.archivePath))
    }

}
