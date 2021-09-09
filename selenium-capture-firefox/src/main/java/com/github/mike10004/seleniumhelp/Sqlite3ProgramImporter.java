package com.github.mike10004.seleniumhelp;

import com.google.common.base.Converter;
import com.google.common.collect.Iterables;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.Subprocess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.mike10004.seleniumhelp.Subprocesses.checkResult;
import static java.util.Objects.requireNonNull;

public class Sqlite3ProgramImporter {

    private static final String DEFAULT_SQLITE_CELL_VALUE = "";

    private static final Logger log = LoggerFactory.getLogger(Sqlite3ProgramImporter.class);

    private final Sqlite3Runner sqliteRunner;
    private final Sqlite3GenericImporter sqlite3Importer;
    private final ExplodedCookieConverter explodedCookieConverter;
    private final FirefoxCookieRowTransform cookieRowTransform;

    public Sqlite3ProgramImporter(Sqlite3Runner sqliteRunner, Sqlite3GenericImporter sqlite3Importer, ExplodedCookieConverter explodedCookieConverter, FirefoxCookieRowTransform cookieRowTransform) {
        super();
        this.sqliteRunner = requireNonNull(sqliteRunner);
        this.sqlite3Importer = requireNonNull(sqlite3Importer);
        this.explodedCookieConverter = requireNonNull(explodedCookieConverter);
        this.cookieRowTransform = requireNonNull(cookieRowTransform);
    }


}
