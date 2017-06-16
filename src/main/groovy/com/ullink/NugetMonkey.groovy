package com.ullink

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.os.OperatingSystem

class NugetMonkey extends Ikvm {
    NugetMonkey() {
        super()
    }

    @TaskAction
    def build() {

        def jsonOutput = "["
        project.configurations.compile.resolvedConfiguration.firstLevelModuleDependencies.each { dep ->
            def addToJson
            addToJson = { resolvedDep ->
                jsonOutput += "\n{"
                jsonOutput += "\"groupId\":\"${resolvedDep.module.id.group}\"," +
                        "\"artifactId\":\"${resolvedDep.module.id.name}\"," +
                        "\"version\":\"${resolvedDep.module.id.version}\"," +
                        "\"file\":\"${resolvedDep.getModuleArtifacts()[0].file.getAbsolutePath().replace("\\", "\\\\")}\""
                jsonOutput += ",\"dependencies\":["
                def depMap = [:]

                if (resolvedDep.children.size() > 0) {
                    resolvedDep.children.each {
                        childResolvedDep ->
//                              println(childResolvedDep.module.id.name)
//                          if (resolvedDep in childResolvedDep.getParents()
//                                  && childResolvedDep.getConfiguration() == 'compile') {
                            addToJson(childResolvedDep)

                            def fl = childResolvedDep.getModuleArtifacts()[0].file;
                            def cd = fl.getName()
                            String cdWe = cd.take(cd.lastIndexOf('.'))

                            String dll = destinationDir.getAbsolutePath()  + "/" + cdWe  + ".dll"
                            depMap.dll = dll
                    }
                    if (jsonOutput[-1] == ',') {
                        jsonOutput = jsonOutput[0..-2]
                    }
                }
                jsonOutput += "]},"

                def workingDep = resolvedDep.getModuleArtifacts()[0].file;
                def curDep = workingDep.getName()
                String fileWithoutExt = curDep.take(curDep.lastIndexOf('.'))

                List<String> lst = new LinkedList<>()
//                lst.add "${rootProject.ext.ikvmpath}\\bin\\ikvmc.exe"
//                lst.add "\"${workingDep.getAbsolutePath()}\""
//                lst.add "-target:library"
//                lst.add "-debug"
//                  lst.add "-nojni"
                depMap.values().each { e ->
                    lst.add e
                }

//                  new FileNameFinder().getFileNames('${rootProject.ext.ikvmpath}\\bin\\',  '*.dll') .each { String fn ->
//                      File f = new File(fn);
//                      if (!f.getName().toLowerCase().contains("native")) {
//                          lst.add "-r:" + f.getAbsoluteFile() //+ ".dll"
//                      }
//                  }
//                  println fileWithoutExt

                new FileNameFinder().getFileNames(destinationDir.getAbsolutePath()  ,  '*.dll') .each { String fn ->
                    File f = new File(fn);
                    def checkDep = f.getName();
                    String fileWithoutExtCheck = checkDep.take(checkDep.lastIndexOf('.'));
//                      println fileWithoutExtCheck
                    if (!f.getName().toLowerCase().contains("native")
                            && f.getAbsolutePath().toLowerCase().endsWith(".dll")
                            && (fileWithoutExt!=(fileWithoutExtCheck))) {
                        lst.add f.getAbsolutePath() //+ ".dll"
                    }
                }
                def home = resolveIkvmHome().getAbsolutePath()
                lst.add "${home}\\bin\\ICSharpCode.SharpZipLib.dll"
                lst.add "${home}\\bin\\IKVM.AWT.WinForms.dll"
                lst.add "${home}\\bin\\IKVM.OpenJDK.Core.dll"
                lst.add "${home}\\bin\\IKVM.OpenJDK.Tools.dll"
                lst.add "${home}\\bin\\IKVM.Reflection.dll"
                lst.add "${home}\\bin\\IKVM.Runtime.JNI.dll"
                lst.add "${home}\\bin\\IKVM.Runtime.dll"
//                  lst.add "${home}\\bin\\IKVM.OpenJDK.Beans.dll "
                def params = lst as String[]

                buildOne(workingDep.getAbsolutePath(),fileWithoutExt,params)

            }
            addToJson(dep)
        }
        if (jsonOutput[-1] == ',') {
            jsonOutput = jsonOutput[0..-2]
        }
        jsonOutput += "]"
//          println jsonOutput
        def myFile = new File("deps.json")
        PrintWriter printWriter = new PrintWriter(myFile)
        printWriter.println(jsonOutput)
        printWriter.close()
    }
    def buildOne(String thisJar,  String name, String[] refsIn) {
        jars thisJar
        assemblyName name
        refs refsIn
        File debugFile = getDestinationDebugFile()
        if (debug && debugFile.isFile()) {
            debugFile.delete();
        }
        project.exec {
            commandLine = commandLineArgs
        }
        if (debug && !debugFile.isFile()) {
            // bug in IKVM 0.40
            File shitFile = new File(getAssemblyName() + ".pdb")
            if (shitFile.isFile()) {
                FileUtils.moveFile(shitFile, debugFile)
            }
        }
        if (generateDoc && !project.gradle.taskGraph.hasTask(project.tasks.ikvmDoc)) {
            project.tasks.ikvmDoc.generate()
        }
    }
}
