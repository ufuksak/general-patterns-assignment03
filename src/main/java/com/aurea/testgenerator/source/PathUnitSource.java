package com.aurea.testgenerator.source;

import com.aurea.testgenerator.config.ProjectConfiguration;
import groovy.transform.Memoized;
import one.util.streamex.StreamEx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class PathUnitSource implements UnitSource {

    private static final Logger logger = LogManager.getLogger(PathUnitSource.class.getSimpleName());

    private final SourceFinder sourceFinder;
    private final PathToUnitMapper pathToUnitMapper;
    private final SourceFilter filter;
    private final Path root;

    public PathUnitSource(SourceFinder sourceFinder, ProjectConfiguration cfg, SourceFilter filter) {
        this.sourceFinder = sourceFinder;
        this.pathToUnitMapper = new PathToUnitMapper(cfg.getSrc());
        this.filter = filter;
        this.root = cfg.getSrc();
    }

    @Override
    public StreamEx<Unit> units(Predicate<Path> anotherFilter) {
        try {
            return sources(anotherFilter)
                    .map(pathToUnitMapper)
                    .filter(Optional::isPresent)
                    .map(Optional::get);
        } catch (IOException e) {
            logger.error("Failed to fetch units", e);
            return StreamEx.empty();
        }
    }

    private StreamEx<Path> sources(Predicate<Path> anotherFilter) throws IOException {
        return sourceFinder.javaClasses().parallel().filter(filter.and(anotherFilter));
    }

    @Override
    @Memoized
    public long size(Predicate<Path> anotherFilter) {
        try {
            return sources(anotherFilter).count();
        } catch (IOException e) {
            logger.error("Failed to fetch units", e);
            return 0;
        }
    }

    @Override
    public String toString() {
        return root + "/**/*.java";
    }
}
