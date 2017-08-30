package com.nugetmonkey

import org.apache.commons.io.FileUtils
import org.codehaus.jackson.map.ObjectMapper
import org.gradle.api.tasks.TaskAction

class NugetMonkey extends Ikvm {
    private static String TEX_IKVMHome = "\$(IKVMHome)"
    private static String TEX_ProjectHome = "\$(ProjectHome)"
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

    def writeTofile(String fileName, String text){
        def myFile = new File(fileName)
        PrintWriter pr = new PrintWriter(myFile)
        try{
            pr.println(text)
        }finally {
            pr.close()
        }
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

        def home = resolveIkvmHome().getAbsolutePath()

        List<String> lstIKVMIKVMc = new LinkedList<>()
        lstIKVMIKVMc.add "${home}\\bin\\ICSharpCode.SharpZipLib.dll"
        lstIKVMIKVMc.add "${home}\\bin\\IKVM.AWT.WinForms.dll"
        lstIKVMIKVMc.add "${home}\\bin\\IKVM.OpenJDK.Core.dll"
        lstIKVMIKVMc.add "${home}\\bin\\IKVM.OpenJDK.Tools.dll"
        lstIKVMIKVMc.add "${home}\\bin\\IKVM.Reflection.dll"
        lstIKVMIKVMc.add "${home}\\bin\\IKVM.Runtime.JNI.dll"
        lstIKVMIKVMc.add "${home}\\bin\\IKVM.Runtime.dll"

        lstIKVMIKVMc.each {
            addOneReference(projFile, new File(it))
        }

        /**
         * http://permalink.gmane.org/gmane.comp.java.ikvm.devel/4085
         */
        List<String> lstIKVMRequiredRefs = new LinkedList<>();
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Beans.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Charsets.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Cldrdata.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Corba.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Jdbc.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Localedata.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Management.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Media.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Misc.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Naming.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Nashorn.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Remoting.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Security.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.SwingAWT.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Text.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.Util.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.XML.API.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.XML.Bind.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.XML.Crypto.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.XML.Parse.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.XML.Transform.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.XML.WebServices.dll"
        lstIKVMRequiredRefs.add "${home}\\bin\\IKVM.OpenJDK.XML.XPath.dll"

        lstIKVMRequiredRefs.each {
            addOneReference(projFile, new File(it))
        }

        String projectVariables = "<Project xmlns=\"http://schemas.microsoft.com/developer/msbuild/2003\">\n" +
                "  <PropertyGroup>\n" +
                "    <IKVMHome>${home}</IKVMHome>\n" +
                "    <ProjectHome>${project.rootDir}</ProjectHome>\n" +
                "  </PropertyGroup>\n" +
                "</Project>"
        writeTofile "ProjectVariables.txt",projectVariables

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

                lst.addAll(lstIKVMIKVMc)

                def params = lst as String[]

                buildOne(workingDep.getAbsolutePath(), fileWithoutExt, params)
            }
            addToJson(dep)
        }
        if (jsonOutput[-1] == ',') {
            jsonOutput = jsonOutput[0..-2]
        }
        jsonOutput += "]"

        writeTofile "deps.json",jsonOutput
    }
    def replaceFolders(String path){
        return path.replace( resolveIkvmHome().getAbsolutePath(), TEX_IKVMHome).replace(project.rootDir.path,TEX_ProjectHome)
    }
    def addOneReference(String projFile, File destFile) {
        // println(projFile)
        if (!projFile.isEmpty() && !projFile.isAllWhitespace()) {
            String refPath = replaceFolders(destFile.path)
            String projectRootReplaced = replaceFolders(project.rootDir.path)
            String projectRoot = project.rootDir.path
            if (System.getProperty('os.name').toLowerCase(Locale.ROOT).contains('windows')) {
                /*project.exec {
                    commandLine 'cmd', '/c', "powershell -command \"" + projectRoot + "/scripts/RemoveReference.ps1 " + projFile + " " + refPath + "\""
                }*/
                project.exec {
                    commandLine 'cmd', '/c', "powershell -command \"" + projectRoot + "/scripts/AddReference.ps1 " + projFile + " " + refPath + " " + destFile.name + "\""
                }
            } else {
/*project.exec {
    commandLine 'sh', '-c', "powershell -command \"" + projectRoot + "/scripts/RemoveReference.ps1 " + projFile + " " + refPath + "\""
}*/
                project.exec {
                    commandLine 'sh', '-c', "powershell -command \"" + projectRoot + "/scripts/AddReference.ps1 " + projFile + " " + refPath + " " + destFile.name + "\""
                }
            }
        }
    }
    def addOneReference() {
        addOneReference(  projFile,   destFile)
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
