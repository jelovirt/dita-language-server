import org.gradle.api.Plugin
import org.gradle.api.Project

class DtdProcessingPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.tasks.register('processDtd') {
            def inputDir = project.file('src/main/resources/schemas/dtd')
            def outputDir = project.layout.buildDirectory.dir('generated/dtd').get().asFile
            inputs.dir inputDir
            outputs.dir outputDir

            doLast {
                def processor = new DtdProcessor()

                project.fileTree(inputDir).include('**/*.dtd').each { dtdFile ->
                    def relativePath = inputDir.toPath().relativize(dtdFile.toPath())
                    def outputFile = outputDir.toPath().resolve(relativePath)
                    outputFile.parent.toFile().mkdirs()

                    project.logger.info("Processing DTD: ${dtdFile.name} -> ${outputFile.fileName}")
                    processor.process(dtdFile.toPath(), outputFile)
                }
            }
        }

        project.tasks.named('processResources') {
            dependsOn 'processDtd'
        }
    }
}