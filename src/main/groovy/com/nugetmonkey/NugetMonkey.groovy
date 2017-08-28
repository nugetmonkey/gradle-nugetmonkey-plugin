package com.nugetmonkey

import org.apache.commons.io.FileUtils
import org.codehaus.jackson.map.ObjectMapper
import org.gradle.api.tasks.TaskAction

class NugetMonkey extends Ikvm {
    NugetMonkey() {
        super()
        /*File debugFile = getDestinationDebugFile()
        outputs.upToDateWhen {
            if (debug) {
                if (debugFile.isFile()
                        && debugFile.exists()) {
                    return true
                }
            }
            if (destFile.isFile()
                    && destFile.exists()) {
                return destFile.exists()
            }
            return false;
        }*/
    }

    @TaskAction
    def build() {
        ObjectMapper mapper = new ObjectMapper();

        // Add dependencies provided through additional dependencies json file.
        File additionalDeps = new File("./AdditionalJavaDependencies.json")
        if (additionalDeps.exists()) {
            GradleObjectModelModifications additionalModel = mapper.readValue(additionalDeps, GradleObjectModelModifications.class)
            if (additionalModel != null) {
                additionalModel.getAdditionalProjectDependencies().each { ad ->
                    //project.afterEvaluate {
                        project.dependencies {
                            "compile" "$ad"
                        }
                    //}
                }
            }
        }
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
                            addToJson(childResolvedDep)

                            def fl = childResolvedDep.getModuleArtifacts()[0].file
                            def cd = fl.getName()
                            String cdWe = cd.take(cd.lastIndexOf('.'))

                            String dll = destinationDir.getAbsolutePath() + "/" + cdWe + ".dll"
                            depMap.dll = dll
                    }
                    if (jsonOutput[-1] == ',') {
                        jsonOutput = jsonOutput[0..-2]
                    }
                }
                jsonOutput += "]},"

                def workingDep = resolvedDep.getModuleArtifacts()[0].file
                def curDep = workingDep.getName()
                String fileWithoutExt = curDep.take(curDep.lastIndexOf('.'))

                List<String> lst = new LinkedList<>()
                depMap.values().each { e ->
                    lst.add e
                }

                new FileNameFinder().getFileNames(destinationDir.getAbsolutePath(), '*.dll').each { String fn ->
                    File f = new File(fn)
                    def checkDep = f.getName()
                    String fileWithoutExtCheck = checkDep.take(checkDep.lastIndexOf('.'))
                    if (!f.getName().toLowerCase().contains("native")
                            && f.getAbsolutePath().toLowerCase().endsWith(".dll")
                            && (fileWithoutExt != (fileWithoutExtCheck))) {
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
                def params = lst as String[]

                buildOne(workingDep.getAbsolutePath(), fileWithoutExt, params)
            }
            addToJson(dep)
        }
        if (jsonOutput[-1] == ',') {
            jsonOutput = jsonOutput[0..-2]
        }
        jsonOutput += "]"
        def myFile = new File("deps.json")
        PrintWriter pr = new PrintWriter(myFile)
        try{
            pr.println(jsonOutput)
        }finally {
            pr.close()
        }
    }

    def addOneReference() {
        // println(projFile)
        if (!projFile.isEmpty() && !projFile.isAllWhitespace()) {
            if (System.getProperty('os.name').toLowerCase(Locale.ROOT).contains('windows')) {
                /*project.exec {
                    commandLine 'cmd', '/c', "powershell -command \"" + project.rootDir.path + "/scripts/RemoveReference.ps1 " + projFile + " " + destFile.path + "\""
                }*/
                project.exec {
                    commandLine 'cmd', '/c', "powershell -command \"" + project.rootDir.path + "/scripts/AddReference.ps1 " + projFile + " " + destFile.path + " " + destFile.name + "\""
                }
            } else {
/*project.exec {
    commandLine 'sh', '-c', "powershell -command \"" + project.rootDir.path + "/scripts/RemoveReference.ps1 " + projFile + " " + destFile.path + "\""
}*/
                project.exec {
                    commandLine 'sh', '-c', "powershell -command \"" + project.rootDir.path + "/scripts/AddReference.ps1 " + projFile + " " + destFile.path + " " + destFile.name + "\""
                }
            }
        }
    }

    def buildOne(String thisJar, String name, String[] refsIn) {
        jars thisJar
        assemblyName name
        refs refsIn
        boolean upToDate = true
        File debugFile = getDestinationDebugFile()
        if (debug) {
            if (!debugFile.isFile()
                    || !debugFile.exists()) {
                upToDate = false
            }
        }
        if (!destFile.isFile()
                || !destFile.exists()) {
            upToDate = false
        }
        if (!upToDate) {
            if (debug && debugFile.isFile()) {
                debugFile.delete()
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
        addOneReference()
    }
}
