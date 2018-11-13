package com.aurea.testgenerator.source

import com.aurea.testgenerator.config.ProjectConfiguration
import groovy.util.logging.Log4j2
import one.util.streamex.StreamEx
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

@Component
@Log4j2
class JavaSourceFinder implements SourceFinder {

    final Path src

    @Autowired
    JavaSourceFinder(ProjectConfiguration cfg) {
        src = cfg.srcPath
        log.info "Source classes are in $src"
    }

    StreamEx<Path> javaClasses() throws IOException {
        if (src.toFile().isFile()) {
            return StreamEx.of(src)
        }
        StreamEx.of(Files.walk(src))
                .filter { Files.isDirectory it }
                .parallel()
                .map { toClasses it }
                .flatMap { it }
    }

    private static Stream<Path> toClasses(Path packageDirectory) {
        try {
            return Files.walk(packageDirectory, 1)
                        .filter { Files.isRegularFile(it) && it.toFile().getName().endsWith(".java") }
        } catch (IOException e) {
            e.printStackTrace()
        }
        Stream.empty()
    }
}
